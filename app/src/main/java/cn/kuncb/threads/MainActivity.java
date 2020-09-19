package cn.kuncb.threads;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import cn.kuncb.threads.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    /**
     * Handler
     * android最重要的两个对象(Context、Handler)之一,怎么强调都不过份
     */
    private Handler handler;
    private TaskThreadPool pools;
    protected ActivityMainBinding bind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.bind = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(this.bind.getRoot());

        this.handler = handlerMessage();

        //实际使用中线程的生命周期应该是onstart中开如,onstop中结束
        //如果有什么需要异步执行的任务,只要把任务加入到队列即可
        this.bind.btnStart.setOnClickListener(new View.OnClickListener() {
            //region click
            @Override
            public void onClick(View v) {
                Editable pool = MainActivity.this.bind.txtPool.getText();
                int poolSize = Integer.parseInt(pool.toString());
                if (poolSize < 1) {
                    Toast.makeText(MainActivity.this, getString(R.string.pool), Toast.LENGTH_LONG).show();
                    return;
                }


                MainActivity.this.pools = new TaskThreadPool(MainActivity.this.handler, poolSize, poolSize * 3, 64);
                MainActivity.this.pools.start();
                MainActivity.this.bind.txtPool.setEnabled(false);
                MainActivity.this.bind.btnStart.setEnabled(false);
                MainActivity.this.bind.btnStop.setEnabled(true);

                try {
                    MainActivity.this.bind.progressBar.setProgress(0);
                    MainActivity.this.bind.progressBar2.setProgress(0);
                    MainActivity.this.bind.progressBar3.setProgress(0);
                    MainActivity.this.bind.progressBar4.setProgress(0);


                    MainActivity.this.pools.enqueue(TaskThreadPool.TASK1);
                    MainActivity.this.pools.enqueue(TaskThreadPool.TASK2);
                    MainActivity.this.pools.enqueue(TaskThreadPool.TASK3);
                    //如果只运行1个线程这里activity会阻塞(无响应),因为队列大小只有3
                    //当队列满了以后之前的任务又未完成就会阻塞等待之前的任务完成.
                    //只要修改队列大小即可解除阻塞状态
                    //当前面的3个任务中任意一个完成之后activity解除阻塞
                    //实际使用中队列大小根据需求设置为32,占用的内存空间至少是 32 * bufferSize
                    //建议类似内存大小,线程数量,队列数量为偶数,不要使用奇数,1除外
                    MainActivity.this.pools.enqueue(TaskThreadPool.TASK4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //endregion
        });


        this.bind.btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != MainActivity.this.pools) {
                    MainActivity.this.pools.stop();
                    MainActivity.this.pools = null;
                }
                MainActivity.this.bind.txtPool.setEnabled(true);
                MainActivity.this.bind.btnStart.setEnabled(true);
                MainActivity.this.bind.btnStop.setEnabled(false);
            }
        });
    }

    /*
    @Override
    protected void onStart() {
        super.onStart();
        this.pools = new TaskThreadPool(this.handler, 1, 32, 128);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (null != this.pools) {
            this.pools.stop();
            this.pools = null;
        }
    }*/


    /**
     * 异步消息处理.google建议的方式
     * 这里是activity线程,可以安全的更新view
     *
     * @return x
     */
    Handler handlerMessage() {
        return new Handler(this.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle bundle = msg.getData();
                switch (msg.what) {
                    case TaskThreadPool.TASK1:
                        do {//傻X的java,这些写是故意的,限制对象的作用域
                            TaskResult result = bundle.getParcelable("task_result");
                            //String s = bundle.getString("string");
                            Log.i("TAG", result.getName());
                            int progress = MainActivity.this.bind.progressBar.getProgress();
                            MainActivity.this.bind.progressBar.setProgress(progress + 10);
                        } while (false);
                        break;
                    case TaskThreadPool.TASK2:
                        do {
                            //TaskResult result = bundle.getParcelable("task_result");
                            int progress = MainActivity.this.bind.progressBar2.getProgress();
                            MainActivity.this.bind.progressBar2.setProgress(progress + 10);
                        } while (false);
                        break;
                    case TaskThreadPool.TASK3:
                        do {
                            //TaskResult result = bundle.getParcelable("task_result");
                            int progress = MainActivity.this.bind.progressBar3.getProgress();
                            MainActivity.this.bind.progressBar3.setProgress(progress + 10);
                        } while (false);
                        break;
                    case TaskThreadPool.TASK4:
                        do {
                            //TaskResult result = bundle.getParcelable("task_result");
                            int progress = MainActivity.this.bind.progressBar4.getProgress();
                            MainActivity.this.bind.progressBar4.setProgress(progress + 10);
                        } while (false);
                        break;
                }

                bundle.clear();
            }
        };
    }
}