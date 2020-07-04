package vn.com.acacy.rxjava;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import vn.com.acacy.rxjava.core.HttpsUtils;

public class MainActivity extends AppCompatActivity {
    private Button btn;
    private TextView txt;
    private RecyclerView lst;
    private ProgressDialog pd;
    private Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
        @Override
        public void subscribe(ObservableEmitter<String> emitter) throws Exception {
            if(!emitter.isDisposed()){
                emitter.onNext(getJSON());
            }
        }
    });

    private Observer<String> observer = new Observer<String>() {
        @Override
        public void onSubscribe(Disposable disposable) {

        }

        @Override
        public void onNext(String s) {
            if(!s.isEmpty()) {
                txt.setText(s);
            }
            pd.dismiss();
            onComplete();
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {
            Log.d("observable-onComplete", "onComplete: ");
        }
    };

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt = findViewById(R.id.txt);
        btn = findViewById(R.id.btn);
        pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Loading....");
        init();
    }

    private void init() {

        btn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CheckResult")
            @Override
            public void onClick(View v) {
                pd.show();
                observable.observeOn(Schedulers.io())
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(observer);

            }
        });
    }

    private String getJSON(){
        String json = "";
        String uri = "https://reqres.in/api/users?page=1";
        try {
            json = HttpsUtils.getHTTPs(uri,null);
        } catch (IOException e) {
            Log.d("HTTP",e.toString());
        }
        return json;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
