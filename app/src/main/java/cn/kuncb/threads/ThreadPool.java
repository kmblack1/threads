package cn.kuncb.threads;


import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程池抽象类
 */
public abstract class ThreadPool {
    private final int poolSize;
    private Thread[] threads;
    private final Queue queue;

    /**
     * 构造方法
     *
     * @param poolSize   线程池大小,大于等于1
     * @param queueMax   队列大小,大于等于1
     * @param bufferSize 参数缓冲区大小,大于等于32
     */
    public ThreadPool(int poolSize, int queueMax, int bufferSize) {
        if (poolSize < 1)
            throw new IllegalArgumentException("poolSize");
        if (queueMax < 1)
            throw new IllegalArgumentException("queueMax");
        if (bufferSize < 32)
            throw new IllegalArgumentException("queueMax");
        this.poolSize = poolSize;
        this.queue = new Queue(poolSize, queueMax, bufferSize);
    }

    /**
     * 启动线程池
     */
    protected void start() {
        this.threads = new Thread[this.poolSize];
        for (int i = 0; i < this.poolSize; ++i) {
            this.threads[i] = new Thread(genreateThread());
            this.threads[i].start();
        }
        //等待线程启动
        this.queue.reentrant.lock();
        try {
            this.queue.condNotFull.await();
            this.queue.isrun = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.queue.reentrant.unlock();
    }

    /**
     * 停止线程池
     */
    protected void stop() {
        this.queue.reentrant.lock();
        this.queue.isrun = false;
        this.queue.cond.signalAll();
        this.queue.condNotFull.signalAll();
        this.queue.reentrant.unlock();
        for (Thread item : this.threads) {
            try {
                item.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.queue.disipose();
    }

    protected void enqueue(int action) throws InterruptedException {
        this.queue.enqueue(action, null);
    }

    /**
     * 执行任务
     * 同时运行的任务数量必须小于等于线程池的数量,否则这个方法会阻塞.
     *
     * @param action     任务id.自行编号
     * @param parcelable 任务参数
     * @throws InterruptedException ?
     */
    protected void enqueue(int action, @NotNull Parcelable parcelable) throws InterruptedException {
        this.queue.enqueue(action, parcelable);
    }

    /**
     * 创建Runnable
     *
     * @return Runnable
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    private Runnable genreateThread() {
        return new Runnable() {
            @Override
            public void run() {
                while (true) {
                    QueueItem item;
                    try {
                        item = ThreadPool.this.queue.dequeue();
                        if (null == item) {
                            break;
                        } else {
                            //region work
                            int action = item.action;
                            Parcel parcel = null;
                            if (item.stream.size() > 0) {
                                parcel = Parcel.obtain();
                                byte[] data = item.stream.toByteArray();
                                parcel.unmarshall(data, 0, data.length);
                                parcel.setDataPosition(0);
                            }
                            threadTask(action, parcel);
                            item.reset();
                            //任务运行完成
                            ThreadPool.this.queue.reentrant.lock();
                            if (!ThreadPool.this.queue.isrun) {
                                ThreadPool.this.queue.reentrant.unlock();
                                break;
                            }
                            //当队列全部使用时,入队会处于wait状态,通知可以入队
                            ThreadPool.this.queue.condNotFull.signal();
                            ThreadPool.this.queue.reentrant.unlock();
                            //endregion
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
    }

    /**
     * 多线程执行任务
     * 注意:执行这个方法的线程是不同的(随机分配)
     *
     * @param action ThreadPool.enqueue传递的action
     * @param parcel ThreadPool.enqueue传递的parcelable
     */
    protected abstract void threadTask(final int action, final Parcel parcel);

    /**
     * 队列是中的项
     */
    private static class QueueItem {
        int action;
        boolean complete = true;
        ByteArrayOutputStreamT stream;

        QueueItem(int bufferSize) {
            this.stream = new ByteArrayOutputStreamT(bufferSize);
        }

        void addAction(int action, @NotNull Parcelable parcelable) {
            this.action = action;
            if (null != parcelable) {
                Parcel parcel = Parcel.obtain();
				//这个方法会额外增加一个类名称(第一项)
                //反序列化时第一个read读取的是类名称
                //可以根据需求来决定具体使用那一个方法
                //parcel.writeParcelable(parcelable, 0);
                parcelable.writeToParcel( parcel, 0);
                try {
                    this.stream.reset();
                    this.stream.write(parcel.marshall());
                    this.stream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        void reset() {
            this.action = 0;
            this.complete = true;
            this.stream.reset();
        }

        /**
         * 释放资源
         */
        void disipose() {
            try {
                if (null != this.stream) {
                    this.stream.close();
                    this.stream = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 队列
     */
    private static class Queue {
        private final int queueMax;
        private final int poolSize;
        private int head = 0;
        private int tail = 0;
        private final QueueItem[] items;


        final ReentrantLock reentrant;
        final Condition cond;
        final Condition condNotFull;
        int threadRunCount = 0;
        boolean isrun = false;


        Queue(int poolSize, int queueMax, int bufferSize) {
            this.poolSize = poolSize;
            this.queueMax = queueMax;
            this.reentrant = new ReentrantLock();
            this.cond = this.reentrant.newCondition();
            this.condNotFull = this.reentrant.newCondition();
            this.items = new QueueItem[this.queueMax];
            for (int i = 0; i < this.queueMax; ++i)
                this.items[i] = new QueueItem(bufferSize);
        }

        /**
         * 释放资源
         */
        void disipose() {
            for (QueueItem item : this.items)
                item.disipose();
        }

        /**
         * 入队
         *
         * @param action     自定义任务id
         * @param parcelable 可序列化的对象
         * @throws InterruptedException ?
         */
        void enqueue(int action, @NotNull Parcelable parcelable) throws InterruptedException {
            QueueItem queueItem;
            int next;
            this.reentrant.lock();
            next = ((this.tail + 1) % this.queueMax);
            queueItem = this.items[this.tail];
            if (!queueItem.complete)
                this.condNotFull.await();
            queueItem.addAction(action, parcelable);
            this.tail = next;
            this.cond.signal();    //发送信号执行任务
            this.reentrant.unlock();
        }

        /**
         * 出队
         *
         * @return 返回null表示线程池已经销毁, 否则正常使用
         * @throws InterruptedException ?
         */
        QueueItem dequeue() throws InterruptedException {
            QueueItem queueItem = null;
            this.reentrant.lock();
            if (this.head == this.tail) {
                //region 主线程在等待线程池中的所有线程启动.启动不会再执行这段代码
                if (this.threadRunCount <= this.poolSize) {
                    ++this.threadRunCount;
                    if (this.threadRunCount == this.poolSize)
                        this.condNotFull.signal();//通知主线程所有线程已经启动
                }
                //endregion
                //等待执行任务
                this.cond.await();
            }
            if (this.isrun) {
                queueItem = this.items[this.head];
                this.head = (this.head + 1) % this.queueMax;
            }
            this.reentrant.unlock();
            return queueItem;
        }
    }
}
