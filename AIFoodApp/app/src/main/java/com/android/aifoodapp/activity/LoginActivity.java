package com.android.aifoodapp.activity;

import static com.android.aifoodapp.interfaceh.baseURL.url;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.aifoodapp.R;
import com.android.aifoodapp.domain.user;
import com.android.aifoodapp.interfaceh.NullOnEmptyConverterFactory;
import com.android.aifoodapp.interfaceh.RetrofitAPI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.User;

import java.security.MessageDigest;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;


public class LoginActivity extends AppCompatActivity {

    Activity activity;
    LinearLayout  btn_google, btn_email;
    ImageView btn_kakao;
    static Context mContext;

    GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_login);
        initialize();
        addListener();
        getKeyHash();


        Function2<OAuthToken, Throwable, Unit> callback = new Function2<OAuthToken, Throwable, Unit>() {
            @Override
            public Unit invoke(OAuthToken oAuthToken, Throwable throwable) {
                if(oAuthToken != null) {
                    //로그인이 성공 (토큰 전달 완료)
                }
                if( throwable != null) {
                    //로그인 실패
                }
                updateKakaoLoginUi();
                return null;
            }
        };

        //1. 카카오 로그인 리스너 등록
        btn_kakao.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
            /*Intent intent = new Intent(activity, MainActivity.class);
            startActivity(intent);*/
                if(UserApiClient.getInstance().isKakaoTalkLoginAvailable(LoginActivity.this)) {
                    //카카오톡 존재시
                    UserApiClient.getInstance().loginWithKakaoTalk(LoginActivity.this, callback);
                }
                else {
                    UserApiClient.getInstance().loginWithKakaoAccount(LoginActivity.this, callback);
                }
            }
        });
        updateKakaoLoginUi();


        /*주희 수정부분 구글 로그인*/
        //TODO 회원 정보가 있으면 -> 메인으로 넘어가고 없으면 intent page로 넘어가서 정보를 입력 받아야 함
        //안드로이드라 처음에 로그인 하고 그냥 안해도 되려나..
        btn_google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_google:
                        signIn();
                        break;
                }
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);



    }
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.


            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url).addConverterFactory(new NullOnEmptyConverterFactory())
                    .addConverterFactory(GsonConverterFactory.create()).build();

            RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);

            retrofitAPI.getUser(account.getId()).enqueue(new Callback<user>() {
                @Override
                public void onResponse(Call<user> call, Response<user> response) {
                    user user = response.body();
                    if(user != null){ // 토큰잉 db에 있는 경우
                        //구글 로그인 연동 종료

                        //main으로 넘어가는경우
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.putExtra("user",user);
                        startActivity(intent);
                    }
                    else{ // 회원가입
                        //servey 화면으로 넘어가는것 --> intent.putExtra로 구글 정보를 넘길지 아니면 현재 로그인 정보를 확인할지 선택
                        Intent intent = new Intent(activity, InitialSurveyActivity.class);
                        startActivity(intent);
                    }

                }

                @Override
                public void onFailure(Call<user> call, Throwable t) {
                    Log.d("dj",call.toString());
                    Log.d("t",t.getMessage());
                    Log.d("TestError!!!!","로그인 실패");
                }
            });


        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Error", "signInResult:failed code=" + e.getStatusCode());
        }
    }
    /*주희 수정부분 구글 로그인 끝*/


    //변수 초기화
    private void initialize(){
        activity = this;
        btn_kakao = (ImageView) findViewById(R.id.btn_kakao);
        btn_google = findViewById(R.id.btn_google);
        btn_email = findViewById(R.id.btn_email);

        //test = findViewById(R.id.textView19);
    }

    //리스너 생성
    private void addListener(){
        //btn_kakao.setOnClickListener(listener_kakao_sign);
        //btn_google.setOnClickListener(listener_google_sign);
        btn_email.setOnClickListener(listener_email_sign);
    }


    //2. 구글 로그인 리스너 등록
    /*
    private final View.OnClickListener listener_google_sign = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(activity, MainActivity.class);
            startActivity(intent);
        }
    };

    */

    //3. 이메일 로그인 리스너 등록
    private final View.OnClickListener listener_email_sign = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(activity, InitialSurveyActivity.class);
            startActivity(intent);
        }
    };

    //카카오
    public void updateKakaoLoginUi() {
        UserApiClient.getInstance().me(new Function2<User, Throwable, Unit>() {
            @Override
            public Unit invoke(User user, Throwable throwable) {
                if ( user != null) { //정상 로그인 & 이미 로그인 되어있을 때

                    String userId = Long.toString(user.getId());
                    String userNickName = user.getKakaoAccount().getProfile().getNickname();
                    //Log.i(TAG, "id " + user.getId());
                    //Log.i(TAG, "invoke: nickname=" + user.getKakaoAccount().getProfile().getNickname());

                    Intent intent = new Intent(activity, MainActivity.class);

                    //intent로 userId값 전달
                    intent.putExtra("kakao_userId", userId);
                    intent.putExtra("kakao_userNickName", userNickName);
                    startActivity(intent);

                    //finish 해도 정상 작동되는지 확인 필요!
                    /*  hanbyul comment:
                        MainActivity에서 로그아웃 버튼을 누르고 다시 앱에 들어가는 경우, 로그아웃 처리가 되지 않는 문제가 발생합니다.
                        아래 finish()를 주석처리하면 문제가 발생하지 않는 것 같아서 일단 주석처리 해두었습니다.
                    */
                    //finish();

                } else {
                    //mobileTextView.setText("로그인 해주세요");
                }
                return  null;
            }
        });
    }

    private void getKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String key = new String(Base64.encode(md.digest(), 0));
                Log.d("Hash key", key);
            }
        } catch (Exception e) {
            Log.e("name not found", e.toString());
        }
    }

}