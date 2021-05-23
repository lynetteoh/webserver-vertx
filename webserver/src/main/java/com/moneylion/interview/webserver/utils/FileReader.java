package com.moneylion.interview.webserver.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReader {

  /**
   * Read file content to string
   * @param path file path
   * @param charset charset used to read file such as ASCII, UTF-8
   * @return file content in string
   * @throws IOException
   */
  public static String fileToString(String path, Charset charset) throws IOException {

    return new String(Files.readAllBytes(Paths.get(path)), charset);
  }

}
