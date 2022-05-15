package com.ptsmods.asmremapper;

import com.google.gson.*;
import com.ptsmods.asmremapper.mappings.*;
import com.ptsmods.asmremapper.util.Descriptor;
import com.ptsmods.asmremapper.util.Pair;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.objectweb.asm.util.ASMifier;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * Main class that does the actual ASMifying and remapping.
 */
public class ASMRemapper {
	private static final Gson gson = new GsonBuilder()
			// Gson can't handle making new instances of Records.
			.registerTypeAdapter(ClassMapping.class, (JsonDeserializer<ClassMapping>) (src, typeOfSrc, context) ->
					new ClassMapping(getString(src, "official"), getString(src, "intermediary"), getString(src, "named")))
			.registerTypeAdapter(MethodMapping.class, (JsonDeserializer<MethodMapping>) (src, typeOfSrc, context) ->
					new MethodMapping(context.deserialize(src.getAsJsonObject().get("owner"), ClassMapping.class), getString(src, "signature"),
							getString(src, "officialSignature"), getString(src, "official"), getString(src, "intermediary"), getString(src, "named")))
			.registerTypeAdapter(FieldMapping.class, (JsonDeserializer<FieldMapping>) (src, typeOfSrc, context) ->
					new FieldMapping(context.deserialize(src.getAsJsonObject().get("owner"), ClassMapping.class), getString(src, "descriptor"),
							getString(src, "officialDescriptor"), getString(src, "official"), getString(src, "intermediary"), getString(src, "named")))
			.create();

	/**
	 * Parses the commandline arguments and invokes the remapping
	 * @param args The commandline arguments
	 * @throws IOException If anything goes wrong when downloading the mappings.
	 */
	public static void main(String[] args) throws IOException {
		OptionParser parser = new OptionParser();
		parser.accepts("help");
		ArgumentAcceptingOptionSpec<String> packageOpt = parser.accepts("package", "The package the output class should be put into.").requiredUnless("help").withRequiredArg();
		ArgumentAcceptingOptionSpec<String> mappingsOpt = parser.accepts("mappings", "Path leading to the mappings file.").requiredUnless("help").withRequiredArg();
		ArgumentAcceptingOptionSpec<String> inputOpt = parser.accepts("input", "Class file or directory to ASMify and remap.").requiredUnless("help").withRequiredArg();
		ArgumentAcceptingOptionSpec<String> outputOpt = parser.accepts("output", "Java file to output the ASM calls to.").requiredUnless("help").withRequiredArg();
		ArgumentAcceptingOptionSpec<String> cacheOpt = parser.accepts("cache", "Directory to store cache.").withRequiredArg();
		ArgumentAcceptingOptionSpec<String> mapUtilOpt = parser.accepts("maputil", "Full name of the class that contains the #map(String, String, String) method to use to map.")
				.requiredUnless("help").withRequiredArg();
		ArgumentAcceptingOptionSpec<String> mapMethodOpt = parser.accepts("mapmethod", "Name of the map method, defaults to map.").withRequiredArg().defaultsTo("map");

		OptionSet options = parser.parse(args);
		if (options.has("help")) {
			parser.printHelpOn(System.out);
			return;
		}

		String pckg = options.valueOf(packageOpt);
		String mappings = options.valueOf(mappingsOpt);
		String input = options.valueOf(inputOpt);
		String output = options.valueOf(outputOpt);
		String cache = options.valueOf(cacheOpt);
		String mapUtil = options.valueOf(mapUtilOpt);
		String mapMethod = options.valueOf(mapMethodOpt);

		String minecraftVer = mappings.substring(mappings.lastIndexOf(File.separatorChar) + 1 + "yarn-".length());
		minecraftVer = minecraftVer.substring(0, minecraftVer.indexOf('+'));

		Path cacheDir = cache == null ? null : Paths.get(cache);
		if (cacheDir != null) {
			if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir);
			if (!Files.isDirectory(cacheDir)) {
				System.err.println("Cache is not a directory, not using cache.");
				cacheDir = null;
			}

			if (cacheDir != null) {
				cacheDir = cacheDir.resolve("ASMRemapper").resolve(Paths.get(mappings).getParent().getFileName().toString());
				Files.createDirectories(cacheDir);
			}
		}

		Mappings yarn = loadYarnMappings(mappings, cacheDir);
		Mappings moj = loadMojMappings(minecraftVer, cacheDir);

		File inputFile = new File(input);
		File outputFile = new File(output);
		if (!inputFile.exists()) {
			System.err.println("Given input file does not exist.");
			return;
		}

		AtomicInteger count = new AtomicInteger();

		if (inputFile.isDirectory()) {
			if (outputFile.exists() && !outputFile.isDirectory()) {
				System.err.println("Output is not a directory while input is.");
				return;
			}

			String absoluteInput = inputFile.getAbsolutePath() + '/';

			Stream<Path> fileStream = Files.walk(inputFile.toPath());
			fileStream
					.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".class"))
					.forEach(path -> {
						try {
							String extra = path.toAbsolutePath().toString().substring(absoluteInput.length()).replace(".class", "Dump.java");
							String classOutput = output + File.separatorChar + extra;

							File outputParent = new File(classOutput).getAbsoluteFile().getParentFile();

							if (!outputParent.exists() && !outputParent.mkdirs()) {
								System.err.println("Could not create parent directories for file " + classOutput);
								return;
							}

							remap(path.toAbsolutePath().toString(), classOutput, yarn, moj, pckg + (extra.contains(File.separator) ? '.' +
									extra.substring(0, extra.lastIndexOf(File.separatorChar)).replace(File.separatorChar, '.') : ""), mapUtil, mapMethod);
							count.incrementAndGet();
						} catch (IOException e) {
							System.err.println("Could not remap file " + path);
							e.printStackTrace();
						}
					});
			fileStream.close();
		} else {
			if (outputFile.exists() && outputFile.isDirectory()) {
				System.err.println("Output is a directory while input is a file.");
				return;
			}

			if (outputFile.getParentFile() != null && !outputFile.getParentFile().mkdirs()) {
				System.err.println("Could not make directory to put output in.");
				return;
			}

			remap(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), yarn, moj, pckg, mapUtil, mapMethod);
			count.incrementAndGet();
		}

		System.out.printf("Successfully remapped %d classfile%s.\n", count.get(), count.get() == 1 ? "" : "s");
	}

	private static void remap(String classFile, String outputFile, Mappings yarn, Mappings moj, String pckg, String mapUtil, String mapMethod) throws IOException {
		PrintStream ogOut = System.out;

		// Capture System output when running ASMifier.
		OutputStream dataStream = new ByteArrayOutputStream();
		System.setOut(new PrintStream(dataStream));

		ASMifier.main(new String[] {classFile});
		System.setOut(ogOut);

		String data = dataStream.toString();

		BiFunction<String, Boolean, Function<MatchResult, String>> methodMatcher = (prefix, appendSC) -> res -> {
			MethodMapping mapping;

			if ("<init>".equals(res.group(3)) || "<clinit>".equals(res.group(3))) mapping = null; // (Static) constructors do not get remapped, obviously.
			else if (yarn.hasMethod(yarn.getClassMapping(res.group(2)), res.group(3), res.group(4)))
				mapping = yarn.getMethodMapping(res.group(2), res.group(3), res.group(4));
			else {
				Class<?> owner = null;
				try {
					owner = Class.forName(res.group(2).replace('/', '.'), false, ASMRemapper.class.getClassLoader());
				} catch (NoClassDefFoundError | ClassNotFoundException ignored) {} // Likely outside source, unlikely that this will require remapping.

				if (owner != null) {
					Descriptor descriptor = parseDescriptor(res.group(4));
					if (owner.isEnum() && ("values".equals(res.group(3)) && descriptor.returnType() == owner.arrayType() && descriptor.parameterTypes().isEmpty() ||
							"valueOf".equals(res.group(3)) && descriptor.returnType() == owner && descriptor.parameterTypes().size() == 1 && descriptor.parameterTypes().get(0) == String.class))
						mapping = null; // Default methods 'values' and 'valueOf' of Enums do not get remapped, obviously.
					else mapping = yarn.getMethodMapping(getDeclaringClass(owner, res.group(3), parseDescriptor(res.group(4)).parameterTypes()).getName().replace('.', '/'),
							res.group(3), res.group(4));
				} else mapping = null;
			}

			return Matcher.quoteReplacement(String.format(prefix + "(%s, \"%s\", %s, \"%s\", %s)" + (appendSC ? ";" : ""), res.group(1), res.group(2),
					mapping == null ? '"' + res.group(3) + '"' : formatMapCall(mapMethod, mapping.intermediary(), res.group(3), moj.getMethodMapping(mapping.owner().official(), mapping.official(), mapping.officialSignature()).named()),
							res.group(4), res.group(5)));
		};

		// Map method instructions
		data = Pattern.compile("methodVisitor\\.visitMethodInsn\\(([A-Z]*), \"(net/minecraft/[A-Za-z\\d/$]*)\", \"(.*)\", \"(.*)\", (.*)\\);")
				.matcher(data)
				.replaceAll(methodMatcher.apply("methodVisitor.visitMethodInsn", true));

		// Map handles
		data = Pattern.compile("new Handle\\(([\\w.]*), \"(net/minecraft/[A-Za-z\\d/$]*)\", \"(.*?)\", \"(.*?)\", (.*?)\\)")
				.matcher(data)
				.replaceAll(methodMatcher.apply("new Handle", false));

		// Map field instructions
		data = Pattern.compile("methodVisitor\\.visitFieldInsn\\(([A-Z]*), \"(net/minecraft/[A-Za-z\\d/$]*)\", \"(.*)\", \"(.*)\"\\);")
				.matcher(data)
				.replaceAll(res -> Matcher.quoteReplacement(String.format("methodVisitor.visitFieldInsn(%s, \"%s\", %s, \"%s\");", res.group(1), res.group(2),
						formatMapCall(mapMethod, yarn.getFieldMapping(res.group(2), res.group(3)).intermediary(), res.group(3), moj.getFieldMapping(yarn.getClassMapping(res.group(2)).official(),
										yarn.getFieldMapping(res.group(2), res.group(3)).official()).named()), res.group(4))));

		// Map inner classes
		data = Pattern.compile("classWriter\\.visitInnerClass\\(\"(net/minecraft/[A-Za-z\\d/]*/[A-Za-z\\d$]*)\", \"(net/minecraft/[A-Za-z\\d/]*/[A-Za-z\\d$]*)\", \"(.*?)\", (.*?)\\);")
				.matcher(data)
				.replaceAll(res -> {
					ClassMapping classYarn = yarn.getClassMapping(res.group(1));
					String intermediary = classYarn.intermediary();
					String mojName = moj.getClassMapping(classYarn.official()).named();

					return Matcher.quoteReplacement(String.format("classWriter.visitInnerClass(\"%s\", \"%s\", %s, %s);", res.group(1), res.group(2),
							formatMapCall(mapMethod, intermediary.substring(intermediary.lastIndexOf('$') + 1),
									res.group(3), mojName.substring(mojName.indexOf('$') + 1)), res.group(4)));
				});

		// Map Minecraft classes
		data = Pattern.compile("(L?)(net/minecraft/[A-Za-z\\d/]*/[A-Za-z\\d$]*)(;?)")
				.matcher(data)
				.replaceAll(res -> "\" + " + Matcher.quoteReplacement(formatMapCall(mapMethod, res.group(1) + yarn.getClassMapping(res.group(2)).intermediary() + res.group(3), res.group(1) + res.group(2) + res.group(3),
						res.group(1) + (yarn.getClassMapping(res.group(2)).isObfuscated() ? moj.getClassMapping(yarn.getClassMapping(res.group(2)).official()).named() : res.group(2)) + res.group(3))) + " + \"");

		data = data
				// Replace package and add import for ASMDump
				.replaceFirst("package (.*?);", String.format("package %s;\nimport static " + mapUtil + "." + mapMethod, pckg))
				// Remove empty string concatenation resulting from earlier replacements.
				.replace(" + \"\"", "").replace("\"\" + ", "");

		// Write result
		try (PrintWriter writer = new PrintWriter(outputFile)) {
			writer.write(data);
			writer.flush();
		}
	}

	private static Mappings loadYarnMappings(String mappings, Path cacheDir) throws IOException {
		if (cacheDir != null && Files.exists(cacheDir) && Files.exists(cacheDir.resolve("yarn.json"))) {
			try {
				return gson.fromJson(new BufferedReader(new InputStreamReader(new FileInputStream(cacheDir.resolve("yarn.json").toAbsolutePath().toString()))), Mappings.class);
			} catch (Exception e) {
				System.err.println("Could not load cache for yarn mappings.");
				e.printStackTrace();
			}
		}

		Map<String, ClassMapping> classes = new HashMap<>();
		Map<String, ClassMapping> oClasses = new HashMap<>();

		Map<Pair<ClassMapping, String>, MethodMapping> methods = new HashMap<>();
		Map<Pair<ClassMapping, String>, FieldMapping> fields = new HashMap<>();

		ZipFile mappingsZip = new ZipFile(new File(mappings));

		List<Pair<Integer, String[]>> rawMappings = new BufferedReader(new InputStreamReader(mappingsZip.getInputStream(mappingsZip.getEntry("mappings/mappings.tiny")))).lines()
				.skip(1)
				.map(s -> Pair.of(s.length() - s.stripLeading().length(), s.trim().split("\t")))
				.toList();
		mappingsZip.close();

		for (Pair<Integer, String[]> rawMapping : rawMappings) {
			String[] mapping = rawMapping.right();
			MappingType type = MappingType.fromChar(mapping[0].charAt(0));

			if (rawMapping.left() == 0 && type == MappingType.CLASS) {
				ClassMapping classMapping = new ClassMapping(mapping[1], mapping[2], mapping.length > 3 ? mapping[3] : mapping[2]); // No mapping yet
				classes.put(mapping.length > 3 ? mapping[3] : mapping[2], classMapping);
				oClasses.put(mapping[1], classMapping);
			}
		}

		ClassMapping lastClass = null;
		Pattern officialClassPatternSig = Pattern.compile("(?<=[();ZBCDFIJS])L([a-z$\\d]*?);");
		Pattern officialClassPatternDesc = Pattern.compile("^L([a-z$\\d]*?);");

		for (Pair<Integer, String[]> rawMapping : rawMappings) {
			String[] mapping = rawMapping.right();
			MappingType type = MappingType.fromChar(mapping[0].charAt(0));
			if (type == MappingType.PARAM) continue;

			if (type == MappingType.CLASS) {
				if (rawMapping.left() == 0) lastClass = classes.get(mapping.length > 3 ? mapping[3] : mapping[2]);
				continue;
			}

			Objects.requireNonNull(lastClass);
			if (type == MappingType.METHOD) {
				String namedSig = officialClassPatternSig.matcher(mapping[1])
						.replaceAll(res -> Matcher.quoteReplacement('L' + oClasses.get(res.group(1)).named() + ';'));

				methods.put(Pair.of(lastClass, mapping[4] + namedSig),
						new MethodMapping(lastClass, namedSig, mapping[1], mapping[2], mapping[3], mapping[4]));
			}
			else if (type == MappingType.FIELD) {
				fields.put(Pair.of(lastClass, mapping[4]),
						new FieldMapping(lastClass, officialClassPatternDesc.matcher(mapping[1])
								.replaceAll(res -> Matcher.quoteReplacement('L' + oClasses.get(res.group(1)).named() + ';')),
								mapping[1], mapping[2], mapping[3], mapping[4]));
			}
		}

		Mappings yarn = new Mappings(Mappings.Type.YARN, classes, methods, fields);

		if (cacheDir != null) {
			Path cacheFile = cacheDir.resolve("yarn.json");

			try (PrintWriter writer = new PrintWriter(cacheFile.toAbsolutePath().toString())) {
				gson.toJson(yarn, writer);
				writer.flush();
			}
		}

		return yarn;
	}

	private static Mappings loadMojMappings(String minecraftVer, Path cacheDir) throws IOException {
		Pattern officialClassPatternDesc = Pattern.compile("^L([a-z$\\d]*?);");

		if (cacheDir != null && Files.exists(cacheDir) && Files.exists(cacheDir.resolve("moj.json"))) {
			try {
				return gson.fromJson(new BufferedReader(new InputStreamReader(new FileInputStream(cacheDir.resolve("moj.json").toAbsolutePath().toString()))), Mappings.class);
			} catch (Exception e) {
				System.err.println("Could not load cache for moj mappings.");
				e.printStackTrace();
			}
		}

		JsonArray versions = gson.fromJson(readPage("https://launchermeta.mojang.com/mc/game/version_manifest.json"), JsonObject.class).get("versions").getAsJsonArray();
		String mojRaw = null;
		for (JsonElement version : versions) {
			JsonObject vo = version.getAsJsonObject();
			if (vo.get("id").getAsString().equals(minecraftVer)) {
				JsonObject client = gson.fromJson(readPage(vo.get("url").getAsString()), JsonObject.class);
				String mojUrl = client.getAsJsonObject("downloads").getAsJsonObject("client_mappings").get("url").getAsString();
				mojRaw = readPage(mojUrl);
				break;
			}
		}

		if (mojRaw == null)
			throw new RuntimeException("Could not get moj mappings for the version the given mappings were built for.");


		Map<String, ClassMapping> classes = new HashMap<>();
		Map<String, ClassMapping> nClasses = new HashMap<>();

		Map<Pair<ClassMapping, String>, MethodMapping> methods = new HashMap<>();
		Map<Pair<ClassMapping, String>, FieldMapping> fields = new HashMap<>();

		String[] mojRawLines = mojRaw.split("\n");

		for (String line : mojRawLines) {
			if (line.startsWith("#")) continue;

			String[] lineA = line.trim().split(" ");
			if (!line.startsWith("    ")) {
				ClassMapping mapping = new ClassMapping(lineA[2].substring(0, lineA[2].length() - 1), null, lineA[0].replace('.', '/'));

				classes.put(mapping.official(), mapping);
				nClasses.put(mapping.named(), mapping);
			}
		}

		ClassMapping currentClass = null;
		for (String line : mojRawLines) {
			if (line.startsWith("#")) continue;

			String[] lineA = line.trim().split(" ");
			if (!line.startsWith("    ")) {
				currentClass = classes.get(lineA[2].substring(0, lineA[2].length() - 1));
				continue;
			}

			Objects.requireNonNull(currentClass); // Should not be possible, but to sate IntelliJ's love for warnings.
			if (lineA[1].contains("(")) {
				String[] paramTypes = lineA[1].substring(lineA[1].indexOf('(') + 1, lineA[1].length() - 1).split(",");
				String retType = lineA[0].contains(":") ? lineA[0].split(":")[2] : lineA[0]; // Interface methods don't contain these numbers.
				String sigRetType = mojParamToSig(retType);
				String officialSig = String.format("(%s)%s", // Official signature
						String.join("", Arrays.stream(paramTypes)
								.map(ASMRemapper::mojParamToSig)
								.map(s -> s.startsWith("L") && s.endsWith(";") ? Optional.ofNullable(nClasses.get(s.substring(1, s.length() - 1))).map(c -> 'L' + c.official() + ';').orElse(s) : s)
								.toArray(String[]::new)),
						sigRetType.startsWith("L") && sigRetType.endsWith(";") ? Optional.ofNullable(nClasses.get(sigRetType.substring(1, sigRetType.length() - 1)))
								.map(c -> 'L' + c.official() + ';').orElse(sigRetType) : sigRetType);

				methods.put(Pair.of(currentClass, lineA[3] + officialSig), new MethodMapping(currentClass,
						String.format("(%s)%s", // Named signature
								String.join("", Arrays.stream(paramTypes)
										.map(ASMRemapper::mojParamToSig)
										.toArray(String[]::new)),
								sigRetType),
						officialSig, lineA[3], null, lineA[1].substring(0, lineA[1].indexOf('('))));
			} else {
				String descriptor = mojParamToSig(lineA[0]);

				fields.put(Pair.of(currentClass, lineA[3]), new FieldMapping(currentClass, descriptor,
						officialClassPatternDesc.matcher(descriptor).matches() ? 'L' + nClasses.get(descriptor.substring(1, descriptor.length() - 1)).official() + ';' : descriptor,
						lineA[3], null, lineA[1]));
			}
		}

		Mappings moj = new Mappings(Mappings.Type.MOJ, classes, methods, fields);

		if (cacheDir != null) {
			Path cacheFile = cacheDir.resolve("moj.json");

			try (PrintWriter writer = new PrintWriter(cacheFile.toAbsolutePath().toString())) {
				gson.toJson(moj, writer);
				writer.flush();
			}
		}

		return moj;
	}

	private static String getString(JsonElement element, String key) {
		return element.getAsJsonObject().get(key) == null ? null : element.getAsJsonObject().get(key).getAsString();
	}

	private static String readPage(String urlString) throws IOException {
		URL url = new URL(urlString);
		Scanner sc = new Scanner(url.openStream());
		sc.useDelimiter("\n");
		StringBuilder sb = new StringBuilder();

		while (sc.hasNext()) sb.append(sc.next()).append('\n');

		return sb.toString();
	}

	private static String mojParamToSig(String paramType) {
		if ("".equals(paramType)) return "";

		int arrayDepth = 0;
		while (paramType.endsWith("[]")) {
			arrayDepth++;
			paramType = paramType.substring(0, paramType.length() - 2);
		}

		StringBuilder param = new StringBuilder(switch (paramType) {
			case "boolean" -> "Z";
			case "byte" -> "B";
			case "char" -> "C";
			case "double" -> "D";
			case "float" -> "F";
			case "int" -> "I";
			case "long" -> "J";
			case "short" -> "S";
			case "void" -> "V"; // For return types
			default -> 'L' + paramType.replace('.', '/') + ';';
		});

		for (int i = 0; i < arrayDepth; i++) param.insert(0, '[');

		return param.toString();
	}

	private static String formatMapCall(String mapMethod, String arg1, String arg2, String arg3) {
		return String.format(mapMethod + "(\"%s\", \"%s\", \"%s\")", arg1.replace("\"", "\\\""), arg2.replace("\"", "\\\""), arg3.replace("\"", "\\\""));
	}

	private static Descriptor parseDescriptor(String descriptor) {
		List<Class<?>> classes = new ArrayList<>();

		boolean readingRetType = false;
		int arrayDepth = 0;
		for (int i = 0; i < descriptor.length(); i++) {
			if (i == 0 && descriptor.charAt(i) == '(') continue;

			if (descriptor.charAt(i) == ')') {
				readingRetType = true;
				continue;
			}

			if (descriptor.charAt(i) == '[') {
				arrayDepth++;
				continue;
			}

			Class<?> c = switch (descriptor.charAt(i)) {
				case 'Z' -> boolean.class;
				case 'B' -> byte.class;
				case 'C' -> char.class;
				case 'D' -> double.class;
				case 'F' -> float.class;
				case 'I' -> int.class;
				case 'J' -> long.class;
				case 'S' -> short.class;
				case 'L' -> {
					int sci = descriptor.substring(i + 1).indexOf(';');
					try {
						String name = descriptor.substring(i + 1, i + 1 + sci).replace('/', '.');

						i += sci + 1;
						yield Class.forName(name, false, ASMRemapper.class.getClassLoader());
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
				case 'V' -> void.class; // For return types
				default -> throw new IllegalStateException("Unexpected value: " + descriptor.charAt(i));
			};
			while (arrayDepth > 0) {
				c = c.arrayType();
				arrayDepth--;
			}

			if (!readingRetType) classes.add(c);
			else return new Descriptor(c, Collections.unmodifiableList(classes));
		}

		throw new IllegalArgumentException("Invalid descriptor");
	}

	private static Class<?> getDeclaringClass(Class<?> owner, String methodName, List<Class<?>> classes) {
		Class<?> c = owner;
		Class<?>[] classesArray = classes.toArray(new Class[0]);

		do {
			owner = c;
			c = getDeclaringClass0(true, owner, methodName, classesArray);
			// E.g. PlayerEntity#getUuid() is declared in the Entity class, but that class gets it from the EntityLike interface.
		} while (c != null);

		return owner;
	}

	private static Class<?> getDeclaringClass0(boolean skipOwner, Class<?> owner, String methodName, Class<?>... classes) {
		if (!skipOwner)
			try {
				owner.getDeclaredMethod(methodName, classes);
				return owner;
			} catch (NoSuchMethodException ignored) {}

		for (Class<?> iface : owner.getInterfaces()) {
			Class<?> c = getDeclaringClass0(false, iface, methodName, classes);
			if (c != null) return c;
		}

		Class<?> sup = owner.getSuperclass();
		if (sup != null)
			return getDeclaringClass0(false, sup, methodName, classes);

		return null;
	}
}
