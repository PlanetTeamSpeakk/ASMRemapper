package com.ptsmods.asmremapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class ASMRemapperTest {

	@Test
	void testRemap() throws IOException {
//		ASMRemapper.main(new String[]{
//				"--mappings=C:\\Users\\tygom\\.gradle\\caches\\modules-2\\files-2.1\\net.fabricmc\\yarn\\1.18+build.1\\e237062b39d2c9f59b98af93a004667713385c1c\\yarn-1.18+build.1-mergedv2.jar",
//				"--input=Compat18.class",
//				"--output=Compat18Dump.java",
//				"--package=com.ptsmods.morecommands.asm.compat"
//		});

		ASMRemapper.main(new String[]{
				"--mappings=C:\\Users\\tygom\\.gradle\\caches\\modules-2\\files-2.1\\net.fabricmc\\yarn\\1.16.5+build.10\\720f58eaf7ac002d2a3b8e22a9bc2894d4db5916\\yarn-1.16.5+build.10-mergedv2.jar",
				"--input=Compat16.class",
				"--output=Compat16Dump.java",
				"--package=com.ptsmods.morecommands.asm.compat",
				"--cache=cache/",
				"--maputil=com.ptsmods.morecommands.asm.ASMDump"
		});
	}
}