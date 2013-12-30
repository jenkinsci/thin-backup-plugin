package org.jenkins.plugins.thinbackup.utils;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class TestFileNameMatcher {

  @Test
  public void simpleMatch() {
    assertTrue(new FileNameMatcher(new File("test")).matches(new File("test")));
    assertTrue(new FileNameMatcher(new File("folder", "test")).matches(new File("test")));
    assertTrue(new FileNameMatcher(new File("test")).matches(new File("folder", "test")));
  }
  
  @Test
  public void simpleNotMatch() {
    assertFalse(new FileNameMatcher(new File("test1")).matches(new File("test")));
    assertFalse(new FileNameMatcher(new File("test")).matches(new File("test1")));
  }
  
  @Test
  public void matchesUsingAssertThat() {
    assertThat(new File("test"), new FileNameMatcher(new File("test")));
    assertThat(new File("test"), new FileNameMatcher(new File("folder", "test")));
    assertThat(new File("folder", "test"), new FileNameMatcher(new File("test")));
  }
  
  @Test
  public void noMatchForNonFileTypes() throws Exception {
    assertFalse(new FileNameMatcher(new File("test1")).matches("test"));
  }

}
