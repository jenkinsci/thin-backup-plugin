package org.jvnet.hudson.plugins.thinbackup.utils;

import java.text.ParseException;

import junit.framework.Assert;

import org.junit.Test;

public class TestUtils {

  @Test
  public void testConvertToDirectoryNameDateFormat() throws ParseException {
    String displayDate = "2011-02-13 10:48";
    String fileDate = Utils.convertToDirectoryNameDateFormat(displayDate);
    Assert.assertEquals("2011-02-13_10-48", fileDate);
  }

  @Test(expected = ParseException.class)
  public void testBadFormatConvertToDirectoryNameDateFormat() throws ParseException {
    String displayDate = "2011-02-13-10:48";
    Utils.convertToDirectoryNameDateFormat(displayDate);
  }

  @Test(expected = ParseException.class)
  public void testWrongFormatConvertToDirectoryNameDateFormat() throws ParseException {
    String displayDate = "FULL-2011-02-13_10-48";
    Utils.convertToDirectoryNameDateFormat(displayDate);
  }

  @Test(expected = ParseException.class)
  public void testEmptyDateConvertToDirectoryNameDateFormat() throws ParseException {
    Utils.convertToDirectoryNameDateFormat("");
  }
}
