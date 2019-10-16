package me.retrodaredevil.solarthing.config.options;

import com.lexicalscope.jewel.cli.Option;

import java.io.File;

/**
 * Represents an option containing the path to a file that determines how the input and output is setup
 */
public interface IOBundleOption {
	@Option(longName = "io", description = "The config file that specifies where the program will get its input from and where its output goes to")
	File getIOBundleFile();
}