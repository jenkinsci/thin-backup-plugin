package org.jvnet.hudson.plugins.thinbackup.hudson.model;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.context.SecurityContextHolder;

import hudson.model.PeriodicWork;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;

/**
 * Duplicated code from <i>hudson.model.AsyncPeriodicWork</i> to reduce the log levels in {@link #doRun()} from INFO to
 * FINEST so the logs are not spamed.
 *
 * All other functionality is exactly the same as in the original class.
 */
public abstract class AsyncPeriodicWork extends PeriodicWork {
  private static final Logger LOG = Logger.getLogger("hudson.plugins.thinbackup");

  /**
   * Name of the work.
   */
  public final String name;

  private Thread thread;

  protected AsyncPeriodicWork(final String name) {
    this.name = name;
  }

  /**
   * Schedules this periodic work now in a new thread, if one isn't already running.
   */
  @Override
  public final void doRun() {
    try {
      if ((thread != null) && thread.isAlive()) {
        LOG.log(Level.WARNING, "{0} thread is still running. Execution aborted.", name);
        return;
      }
      thread = new Thread(() -> {
        LOG.log(Level.FINEST, "Started {0}", name);
        final long startTime = System.currentTimeMillis();

        final StreamTaskListener l = createListener();
        try {
          SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
          execute(l);
        } catch (final IOException e1) {
          LOG.log(Level.SEVERE, e1.getMessage());
        } catch (final InterruptedException e2) {
          LOG.log(Level.SEVERE, "interruped");
          Thread.currentThread().interrupt();
        } finally {
          l.closeQuietly();
        }
        LOG.log(Level.FINEST, "Finished {0}. {1} ms", new Object[] { name, (System.currentTimeMillis() - startTime) });
      }, name + " thread");
      thread.start();
    } catch (final Exception e) {
      LOG.log(Level.SEVERE, String.format("%s thread failed with error", name), e);
    }
  }

  protected StreamTaskListener createListener() {
    try {
      return new StreamTaskListener(getLogFile());
    } catch (final IOException e) {
      throw new Error(e);
    }
  }

  /**
   * Determines the log file that records the result of this task.
   *
   * @return log file
   */
  protected File getLogFile() {
    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins != null) {
      return new File(jenkins.getRootDir(), name + ".log");
    } else {
      return null;
    }
  }

  /**
   * Executes the task. Subclasses implement this method and can carry out a long-running task.
   *
   * @param listener Output sent will be reported to the users. (this work is TBD.)
   * @throws InterruptedException The caller will record the exception and moves on.
   * @throws IOException The caller will record the exception and moves on.
   */
  protected abstract void execute(TaskListener listener) throws IOException, InterruptedException;

}
