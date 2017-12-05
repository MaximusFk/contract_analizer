package com.contractanalizer.solidity;

import java.io.File;
import java.util.Arrays;

import org.web3j.codegen.SolidityFunctionWrapperGenerator;

public class SolGen {
	
	private static String binPath;
	private static String outputPath;
	private static String packageName;

	public static void main(String[] args) {
		if(args.length >= 3) {
			binPath = args[0];
			outputPath = args[1];
			packageName = args[2];
			File currentPath = new File(binPath);
			String[] solidityBinaries = currentPath.list((file, name) -> {
				return name.endsWith(".bin");
			});
			Arrays.stream(solidityBinaries)
				  .map(name -> { return name.substring(0, name.length() - 4); })
				  .forEach(SolGen::generateWrapper);
		}
		else {
			throw new java.lang.IllegalArgumentException("Need 3 arguments (binary path, output path, package name)");
		}
	}
	
	private static void generateWrapper(String contractName) {
		String[] params = new String[] {
				"--javaTypes",
				binPath + "/" + contractName + ".bin",
				binPath + "/" + contractName + ".abi",
				"-o",
				outputPath,
				"-p",
				packageName
		};
		
		try {
			SolidityFunctionWrapperGenerator.main(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
