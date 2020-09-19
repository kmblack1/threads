package cn.kuncb.threads;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;

public class TaskThreadPool extends ThreadPool {
    //自定义ACTION ID
    public static final int TASK1 = 1;
    public static final int TASK2 = 2;
    public static final int TASK3 = 3;
    public static final int TASK4 = 4;
    /**
     * Handler
     * android最重要的两个对象(Context、Handler)之一,怎么强调都不过份
     */
    private Handler handler;


    /**
     * 构造方法
     *
     * @param poolSize   线程池大小,大于等于1
     * @param queueMax   队列大小,大于等于1
     * @param bufferSize 参数缓冲区大小,大于等于32
     */
    public TaskThreadPool(Handler handler, int poolSize, int queueMax, int bufferSize) {
        super(poolSize, queueMax, bufferSize);
        this.handler = handler;
    }

    // 多线程执行任务
    // 再次强调:执行这个方法的线程是不同的(随机分配).
    // 如果只使用一个线程的话就是排队
    @Override
    protected void threadTask(final int action, final Parcel parcel) {
        //这里为演示方便action都统一执行一个方法,不使用parcel参数
        //实际使用不同的action应该执行不同的方法,且参数也不相同
        switch (action) {
            case TASK1:
                taskDemo(action, parcel);
                break;
            case TASK2:
                taskDemo(action, parcel);
                break;
            case TASK3:
                taskDemo(action, parcel);
                break;
            case TASK4:
                taskDemo(action, parcel);
                break;
        }
    }

    //这里线程池中的一个线程
    private void taskDemo(final int action, final Parcel parcel) {
        for (int i = 0; i < 10; ++i) {
            try {
                Thread.sleep(500);
                Message msg = TaskThreadPool.this.handler.obtainMessage();
                /*
                 * 为保证线程安全,执行的任务参数必须实现Parcelable接口
                 * 如果需要传递对象,则对象也必须实现Parcelable接口
                 * 如果要获取执行结果,
                 */
                TaskResult result = new TaskResult(String.format("线程id:%d,任务id:%d", Thread.currentThread().getId(), action));
                Bundle bundle = msg.getData();
                bundle.putParcelable("task_result", result);
                bundle.putString("string", "hello");
                msg.what = action;
                msg.sendToTarget();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
