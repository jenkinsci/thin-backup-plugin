package org.jenkins.plugins.thinbackup.strategies;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import jenkins.model.Jenkins;
import hudson.ExtensionList;

import org.hamcrest.Matchers;
import org.junit.Test;

public class TestStrategy {

  @Test
  public void noJenkinsFound() {
    ExtensionList<Strategy> list = Strategy.all(null);
    
    assertThat(list, Matchers.notNullValue(ExtensionList.class));
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void foundExtensions() {
    ExtensionList<Strategy> mockList = mock(ExtensionList.class);
    Jenkins mockJenkins = mock(Jenkins.class);
    when(mockJenkins.getExtensionList(org.mockito.Matchers.isA(Class.class))).thenReturn(mockList);
    
    ExtensionList<Strategy> list = Strategy.all(mockJenkins);
    
    verify(mockJenkins).getExtensionList(org.mockito.Matchers.isA(Class.class));
    assertThat(list, Matchers.notNullValue(ExtensionList.class));
  }

}
