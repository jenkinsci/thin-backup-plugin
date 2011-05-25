package org.jvnet.hudson.plugins.thinbackup.hudson.model;

import hudson.model.PeriodicWork;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.security.ACL;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.acegisecurity.context.SecurityContextHolder;

/**
 * duplicated because the log levels in {@link #doRun()} are reduced from INFO to FINEST to not spam the logs every
 * minute.
 * 
 * {@link PeriodicWork} that takes a long time to run.
 * 
 * <p>
 * Subclasses will implement the {@link #execute(TaskListener)} method and can carry out a long-running task. This runs
 * in a separate thread so as not to block the timer thread, and this class handles all those details.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AsyncPeriodicWork extends PeriodicWork {
  /**
   * Name of the work.
   */
  public final String name;

  private Thread thread;

  protected AsyncPeriodicWork(String name) {
    this.name = name;
  }

  /**
   * Schedules this periodic work now in a new thread, if one isn't already running.
   */
  @Override
  public final void doRun() {
    try {
      if (thread != null && thread.isAlive()) {
        logger.log(Level.WARNING, name + " thread is still running. Execution aborted.");
        return;
      }
      thread = new Thread(new Runnable() {
        public void run() {
          logger.log(Level.FINEST, "Started " + name);
          long startTime = System.currentTimeMillis();

          StreamTaskListener l = createListener();
          try {
            SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);

            execute(l);
          } catch (IOException e) {
            e.printStackTrace(l.fatalError(e.getMessage()));
          } catch (InterruptedException e) {
            e.printStackTrace(l.fatalError("aborted"));
          } finally {
            l.closeQuietly();
          }

          logger.log(Level.FINEST, "Finished " + name + ". " + (System.currentTimeMillis() - startTime) + " ms");
        }
      }, name + " thread");
      thread.start();
    } catch (Throwable t) {
      logger.log(Level.SEVERE, name + " thread failed with error", t);
    }
  }

  protected StreamTaskListener createListener() {
    try {
      return new StreamTaskListener(getLogFile());
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  /**
   * Determines the log file that records the result of this task.
   */
  protected File getLogFile() {
    return new File(Hudson.getInstance().getRootDir(), name + ".log");
  }

  /**
   * Executes the task.
   * 
   * @param listener
   *          Output sent will be reported to the users. (this work is TBD.)
   * @throws InterruptedException
   *           The caller will record the exception and moves on.
   * @throws IOException
   *           The caller will record the exception and moves on.
   */
  protected abstract void execute(TaskListener listener) throws IOException, InterruptedException;
}
