package com.vicmatskiv.pointblank.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

public class ServerTaskScheduler implements Runnable {
   private DelayQueue<DelayedTask> queue = new DelayQueue();

   public void run() {
      Collection<DelayedTask> tasks = new ArrayList();
      this.queue.drainTo(tasks);
      Iterator var2 = tasks.iterator();

      while(var2.hasNext()) {
         DelayedTask task = (DelayedTask)var2.next();
         task.run();
      }

   }

   public void scheduleDelayedTask(Runnable task, long delayMillis) {
      this.queue.add(new DelayedTask(task, delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS));
   }

   private static class DelayedTask implements Delayed, Runnable {
      private final long startDelayTime;
      private Runnable task;

      DelayedTask(Runnable task, long delay, java.util.concurrent.TimeUnit timeUnit) {
         this.task = task;
         this.startDelayTime = System.currentTimeMillis() + java.util.concurrent.TimeUnit.MILLISECONDS.convert(delay, timeUnit);
      }

      public long getDelay(java.util.concurrent.TimeUnit unit) {
         long delay = this.startDelayTime - System.currentTimeMillis();
         return unit.convert(delay, java.util.concurrent.TimeUnit.MILLISECONDS);
      }

      public int compareTo(Delayed other) {
         if (this.startDelayTime < ((DelayedTask)other).startDelayTime) {
            return -1;
         } else {
            return this.startDelayTime > ((DelayedTask)other).startDelayTime ? 1 : 0;
         }
      }

      public void run() {
         this.task.run();
      }
   }
}
