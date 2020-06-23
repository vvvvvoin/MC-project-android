package com.example.semiproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import communication.SharedObject;
import communication.WeatherService;
import event.BackPressCloseHandler;
import model.AirconditionerVO;
import model.AirpurifierVO;
import model.DoorVO;
import model.LightVO;
import model.LogVO;
import model.SensorDataVO;
import model.SystemInfoVO;
import model.WeatherVO;
import model.WindowVO;
import recyclerViewAdapter.ViewType;
import viewPage.FragmentAirConditioner;
import viewPage.FragmentHome;
import viewPage.FragmentLight;
import viewPage.FragmentSetting;
import viewPage.FragmentWindow;


public class MainActivity extends AppCompatActivity {
    String TAG = "MainActivity";
    String name = "/ID:ANDROID";

    String user_email = "";
    FirebaseUser user;
    FirebaseAuth.AuthStateListener mAuthStateListener;
    RecyclerView recyclerVIew;
    ViewPager viewPager;
    TabLayout tabLayout;
    FrameLayout flFirstVIew;
    FrameLayout frame;

    Intent intent;
    Intent serviceIntent;
    Bundle bundle;
    SensorDataVO sensorDataVO = new SensorDataVO();
    WeatherVO weatherVO;
    WeatherVO[] weathers;
    //Fragment
    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;
    FragmentHome fragmentHome;
    FragmentWindow fragmentWindow;

    FragmentAirConditioner fragmentAirConditioner;
    FragmentSetting fragmentSetting;
    FragmentLight fragmentLight;
    int fragmentTag = 0;
    ArrayList<SystemInfoVO> list;
    ArrayList<SystemInfoVO> listFragmentWindow;
    //Socket Communication
    Socket socket;
    PrintWriter printWriter;
    BufferedReader bufferedReader;
    ObjectMapper objectMapper = new ObjectMapper();

    SharedObject sharedObject = new SharedObject();

    String jsonData;
    //Speech recognition
    SpeechRecognizer speechRecognizer;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    // recycler_item_weatherinfo 관련
    TextView roomTemp;
    ImageView outWeather;
    ImageView roomPM;

    SwipeRefreshLayout swipeRefresh;
    Thread pattenThread;
    int cntPatten = 0;
    double lastClapTime = 0;
    AudioDispatcher dispatcher;
    PercussionOnsetDetector mPercussionDetector;

    BackPressCloseHandler backPressCloseHandler;

    boolean voiceRecognition;
    boolean orderVoiceRecognition = false;
    private SharedPreferences appData;
    SharedPreferences.Editor editor;

    TextToSpeech tts;       //음석 출력관련 변수 선언

    boolean singleResult = false;

    LogVO logVO = new LogVO();
    ArrayList<AirconditionerVO> airconditionerData = new ArrayList<AirconditionerVO>();
    ArrayList<AirpurifierVO> airpurifierData = new ArrayList<AirpurifierVO>();
    ArrayList<DoorVO> doorData = new ArrayList<DoorVO>();
    ArrayList<WindowVO> windowData = new ArrayList<WindowVO>();
    ArrayList<LightVO> lightData = new ArrayList<LightVO>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LoginActivity a = (LoginActivity)LoginActivity.loginActivity;
        a.finish();

        user = FirebaseAuth.getInstance().getCurrentUser();
        //RecyclerView Item List 생성성//
        initRecyclerAdapter();
        //Service Start//
        serviceIntent = new Intent(getApplicationContext(), WeatherService.class);
        startService(serviceIntent);
        //Communication Thread Start//
        thread.start();
        //Voice Recognition Thread Start
        appData = getApplicationContext().getSharedPreferences("appData", getApplicationContext().MODE_PRIVATE);
        editor = appData.edit();
        voiceRecognitionCheck.start();

        //음석인식 변수 정의
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != android.speech.tts.TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });
        /**
         * Implementing Pull to Refresh
         * WeatherService Restart
         */
        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(onRefreshListener);

        /**
         * App 실행시 처음 표시해줄 Fragment
         * 선언해 주지 않으면 MainActivity 의 빈 화면이 보이게 된다
         */
        fragmentManager = getSupportFragmentManager();
        if (fragmentAirConditioner == null) {
            fragmentAirConditioner = new FragmentAirConditioner(sharedObject, bufferedReader, this.sensorDataVO);
        }
        if (fragmentHome == null) {
            weatherVO = new WeatherVO();
            fragmentTransaction = fragmentManager.beginTransaction();
            bundle = new Bundle();
            fragmentHome = new FragmentHome(sharedObject, bufferedReader, this.sensorDataVO);
            bundle.putSerializable("list", list);
            bundle.putSerializable("weather", weatherVO);
            bundle.putSerializable("sensorData", sensorDataVO);
            fragmentHome.setArguments(bundle);
            fragmentTransaction.replace(
                    R.id.frame, fragmentHome).commitAllowingStateLoss();
        }
        /**
         * //TabLayout 항목 추가(추가 항목 수에따라 TabLayout 항목이 생성)
         * TabLayout SelectListenerEvent
         */
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().
                setCustomView(createTabView(R.drawable.house_black_18dp)));
        tabLayout.addTab(tabLayout.newTab().
                setCustomView(createTabView(R.drawable.toys_black_18dp)));
        tabLayout.addTab(tabLayout.newTab().
                setCustomView(createTabView(R.drawable.voice_black)));
        tabLayout.addTab(tabLayout.newTab().
                setCustomView(createTabView(R.drawable.ic_windy)));
        tabLayout.addTab(tabLayout.newTab().
                setCustomView(createTabView(R.drawable.settings_black)));
        tabLayout.addOnTabSelectedListener(mTabSelect);

        /**
         * //////////////////Speech recognition/////////////////////
         */
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.RECORD_AUDIO)
//                != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                    Manifest.permission.RECORD_AUDIO)) {
//            } else {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO
//                );
//            }
//        }

        try {
            intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
            //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, new Long(2000));
        } catch (Exception e) {
            Log.v(TAG, "RecognizerIntent Exception==" + e);
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(recognitionListener);

        //패턴인식 레코그니션 실행
        pattenRecognition(intent);

        //onBackPressed Event 객체 생성
        backPressCloseHandler = new BackPressCloseHandler(this);

        //        frame=findViewById(R.id.frame);
//        frame.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
//            public void onSwipeTop() {
//                Log.v(TAG,"onSwipeTop()");
//                speechRecognizer.startListening(intent);
//            }
//            public void onSwipeRight() {
//                Log.v(TAG,"onSwipeRight()");
//            }
//            public void onSwipeLeft() {
//                Log.v(TAG,"onSwipeLeft()");
//            }
//            public void onSwipeBottom() {
//                Log.v(TAG,"onSwipeBottom()");
//                Log.v(TAG,"onRefresh()_Fragment=="+getSupportFragmentManager().getFragments().toString());
//                for (Fragment currentFragment : getSupportFragmentManager().getFragments()) {
//                    if (currentFragment.isVisible()) {
//                        if(currentFragment instanceof FragmentHome){
//                            Log.v(TAG,"FragmentHome");
//                            startService(serviceIntent);
//                        }else if (currentFragment instanceof FragmentWindow){
//                            fragmentTransaction = fragmentManager.beginTransaction();
//                            if (fragmentWindow == null) {
//                                fragmentWindow = new FragmentWindow(sharedObject);
//                            }
//                            fragmentTransaction.replace(
//                                    R.id.frame, fragmentWindow).commitAllowingStateLoss();
//                            bundle.putSerializable("weather", weatherVO);
//                            bundle.putSerializable("window", windowVO);
//                            fragmentWindow.setArguments(bundle);
//                            Log.v(TAG,"FragmentWindow_OnRefreshListener");
//                        }
//                        else if (currentFragment instanceof FragmentSetting){
//                            Log.v(TAG,"FragmentSetting");
//                        }
//                        else if (currentFragment instanceof FragmentTest){
//                            Log.v(TAG,"FragmentTest");
//                        }
//                        else if (currentFragment instanceof FragmentLight){
//                            Log.v(TAG,"FragmentLight");
//                        }
//                    }
//                }
//            }
//        });
        //ViewPager Code//
//        viewPager = findViewById(R.id.viewPager);
//        ContentViewPagerAdapter pagerAdapter = new ContentViewPagerAdapter(getSupportFragmentManager());
//        viewPager.setAdapter(pagerAdapter);
//        tabLayout=findViewById(R.id.tabLayout);
//        tabLayout.setupWithViewPager(viewPager);

//        Communication.DataReceveAsyncTask111 asyncTaskTest =
//                new Communication.DataReceveAsyncTask111(objectInputStream, windowVO);
//        asyncTaskTest.execute();


    }

    /**
     * FragmentHome의  RecyclerView에 표시할 데이터 정보 Method
     */
    public void initRecyclerAdapter() {
        list = new ArrayList<>();
        list.add(new SystemInfoVO(
                R.drawable.angry, "대기상태", "좋음", ViewType.ItemVerticalWeather));
        list.add(new SystemInfoVO(
                R.drawable.smart, "스마스 모트",  ViewType.ItemVertical));
        list.add(new SystemInfoVO(
                R.drawable.sleep, "수면 모드",  ViewType.ItemVertical));
        list.add(new SystemInfoVO(
                R.drawable.ic_windy, "환기 모드",  ViewType.ItemVertical));
        list.add(new SystemInfoVO(
                R.drawable.outing, "외출 모드",  ViewType.ItemVertical));

        listFragmentWindow = new ArrayList<>();
        listFragmentWindow.add(new SystemInfoVO("대기질상태", ViewType.ItemVerticalAir));
        listFragmentWindow.add(new SystemInfoVO(R.drawable.window1, "대기질컨트롤", ViewType.ItemVerticalAirControl));

    }


    /**
     * 인자를 받아 Custom TabLayout 생성하는 Method
     *
     * @param iconImage
     * @return
     */
    private View createTabView(int iconImage) {
        View tabView = getLayoutInflater().inflate(R.layout.custom_tab, null);
//        TextView tvTab = (TextView) tabView.findViewById(R.id.tvTab);
//        tvTab.setText(tabName);
        ImageView ivTab = (ImageView) tabView.findViewById(R.id.ivTab);
        ivTab.setImageResource(iconImage);
        return tabView;
    }

    /**
     * Service 를 이용해 webServer 에서 REST API 통신을 이용 데이터를 가져온다
     */
    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(TAG, "onNewIntent()_intent.getExtras()==" + intent.getExtras().get("weatherResult").toString());
        weathers = (WeatherVO[]) intent.getExtras().get("weatherResult");
        Log.v(TAG, "onNewIntent()_weathers[0].getTemp()==" + weathers[0].getTemp());
        Log.v(TAG, "onNewIntent()_weathers[0].getPm25Value()==" + weathers[0].getPm25Value());
        weatherVO = weathers[0];
        weatherVO.checkElement();

        //아래꺼에서 이걸로 교체함

        for (Fragment currentFragment : getSupportFragmentManager().getFragments()) {
            if (currentFragment.isVisible()) {
                if (currentFragment instanceof FragmentHome) {
                    fragmentTransaction = fragmentManager.beginTransaction();
                    bundle = new Bundle();
                    fragmentHome = new FragmentHome(sharedObject, bufferedReader, this.sensorDataVO);
                    bundle.putSerializable("list", list);
                    bundle.putSerializable("weather", weatherVO);
                    bundle.putSerializable("sensorData", sensorDataVO);
                    Log.v(TAG, "WeatherTEST" + sensorDataVO.getTemp());
                    fragmentHome.setArguments(bundle);
                    fragmentTransaction.replace(
                            R.id.frame, fragmentHome).commitAllowingStateLoss();
                }
            }
        }

        // WebServer로 부터 가져온 데이터를 Fragment 를 생성하면서 Fragment 에 데이터를 넘겨준다
        /*
        fragmentTransaction = fragmentManager.beginTransaction();
        bundle = new Bundle();
        fragmentHome = new FragmentHome(sharedObject, bufferedReader, this.sensorDataVO);
        bundle.putSerializable("list", list);
        bundle.putSerializable("weather", weatherVO);
        bundle.putSerializable("sensorData", sensorDataVO);
        Log.v(TAG, "WeatherTEST" + sensorDataVO.getTemp());
        fragmentHome.setArguments(bundle);
        fragmentTransaction.replace(
                R.id.frame, fragmentHome).commitAllowingStateLoss();

         */
        super.onNewIntent(intent);
    }

    /**
     * BackButton Pressed
     */
    @Override
    public void onBackPressed() {
        Log.v(TAG, "onBackPressed() == IN");
        backPressCloseHandler.onBackPressed();
    }

    /**
     * Server Socket Client Remove
     */
    @Override
    protected void onDestroy() {
        /* TODO: FragmentSetting에 있는 sharedObject, 여기에 있는 sharedObject, 둘 다 필요?*/
        sharedObject.put(name + user_email + " OUT");
        Log.v(TAG, "onDestroy()");
        try {
            printWriter.close();
            bufferedReader.close();
        } catch (Exception e) {
            Log.v(TAG, "onDestroy()_bufferedReader.close()_IOException==" + e.toString());
        }
        super.onDestroy();
    }

    /**
     * Socket Communication witA Server
     */
    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                socket = new Socket("70.12.60.98", 1357);
                bufferedReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                printWriter = new PrintWriter(socket.getOutputStream());

                Log.v(TAG, "Socket Situation==" + socket.isConnected());
                name = name.trim();
                user = FirebaseAuth.getInstance().getCurrentUser();
                user_email = user.getEmail();
                sharedObject.put(name + user_email + " IN");
                Log.v(TAG, "user name ==" + user_email);

                sharedObject.put("/ANDROID>/LOG");

//                sharedObject.put(user_email);

                Log.v(TAG, "name==" + name);
//                    Communication.DataReceveAsyncTask asyncTask =
//                            new Communication.DataReceveAsyncTask(bufferedReader);
//                    asyncTask.execute();
                Thread thread1 = new Thread(new Runnable() {
                    private SensorDataVO sensorDataVO2;

                    @Override
                    public void run() {
                        //sensorDataVO = new SensorDataVO();
                        //fragmentWindow = new FragmentWindow(sharedObject, bufferedReader, sensorDataVO, weatherVO);
                        while (true) {
                            try {
                                jsonData = bufferedReader.readLine();
                                Log.v(TAG, "jsonDataReceive==" + jsonData);
                                if (jsonData != null) {

                                    if(jsonData.contains("mode")){
                                        sensorDataVO2 = objectMapper.readValue(jsonData, SensorDataVO.class);

                                        sensorDataVO.setMode(sensorDataVO2.getMode());
                                        sensorDataVO.setAirconditionerMode(sensorDataVO2.getAirconditionerMode());
                                        sensorDataVO.setAirconditionerSpeed(sensorDataVO2.getAirconditionerSpeed());
                                        sensorDataVO.setAirconditionerStatus(sensorDataVO2.getAirconditionerStatus());
                                        sensorDataVO.setAirconditionerTemp(sensorDataVO2.getAirconditionerTemp());
                                        sensorDataVO.setAirpurifierStatus(sensorDataVO2.getAirpurifierStatus());
                                        sensorDataVO.setDust10(sensorDataVO2.getDust10());
                                        sensorDataVO.setDust25(sensorDataVO2.getDust25());
                                        sensorDataVO.setGasStatus(sensorDataVO2.getGasStatus());
                                        sensorDataVO.setLightStatus(sensorDataVO2.getLightStatus());
                                        sensorDataVO.setTemp(sensorDataVO2.getTemp());
                                        sensorDataVO.setWindowStatus(sensorDataVO2.getWindowStatus());

                                        bundle.putSerializable("sensorData", sensorDataVO);
                                        Log.v(TAG, sensorDataVO.toString());
                                    }else {
                                        JSONObject jsonObject = new JSONObject(jsonData);
                                        Log.v(TAG,"JSON TEST  jsonObject.length()=="+ jsonObject.length());
                                        JSONArray airconditionerList = jsonObject.getJSONArray("airconditionerList");
                                        JSONArray airpurifierList = jsonObject.getJSONArray("airpurifierList");
                                        JSONArray doorList = jsonObject.getJSONArray("doorList");
                                        JSONArray lightList = jsonObject.getJSONArray("lightList");
                                        JSONArray windowList = jsonObject.getJSONArray("windowList");
                                        Log.v(TAG,"JSON TEST  airconditionerList.length()=="+ airconditionerList.length());
                                        Log.v(TAG,"JSON TEST  airconditionerStatus=="+ airconditionerList.getJSONObject(0).getString("airconditionerStatus"));
                                        Log.v(TAG,"JSON TEST  airpurifierList.length()=="+ airpurifierList.length());
                                        Log.v(TAG,"JSON TEST  doorList.length()=="+ doorList.length());
                                        Log.v(TAG,"JSON TEST  lightList.length()=="+ lightList.length());
                                        Log.v(TAG,"JSON TEST  windowList.length()=="+ windowList.length());

                                        for(int i = 0; i < airconditionerList.length(); i++){
                                            Log.v(TAG,"JSON TEST airconditionerList i=="+i);
                                            AirconditionerVO airconditionerVO = new AirconditionerVO();
                                            JSONObject airconditionerListData = airconditionerList.getJSONObject(i);
                                            airconditionerVO.setAirconditionerStatus(airconditionerListData.getString("airconditionerStatus"));
                                            airconditionerVO.setAirconditionerTime(airconditionerListData.getString("airconditionerTime"));
                                            airconditionerData.add(airconditionerVO);
                                            logVO.setAirconditionerList(airconditionerData);
                                            Log.v(TAG,"JSON TEST airconditionerList=="+airconditionerListData);
                                        }
                                        for(int i = 0; i < airpurifierList.length(); i++){
                                            Log.v(TAG,"JSON TEST airpurifierList i=="+i);
                                            AirpurifierVO airpurifierVO = new AirpurifierVO();
                                            JSONObject airpurifierListData = airpurifierList.getJSONObject(i);
                                            airpurifierVO.setAirpurifierStatus(airpurifierListData.getString("airpurifierStatus"));
                                            airpurifierVO.setAirpurifierTime(airpurifierListData.getString("airpurifierTime"));
                                            airpurifierData.add(airpurifierVO);
                                            logVO.setAirpurifierList(airpurifierData);
                                            Log.v(TAG,"JSON TEST airpurifierListData=="+airpurifierListData);
                                        }
                                        for(int i = 0; i < doorList.length(); i++){
                                            Log.v(TAG,"JSON TEST doorList i=="+i);
                                            DoorVO doorVO = new DoorVO();
                                            JSONObject doorListData = doorList.getJSONObject(i);
                                            doorVO.setDoorStatus(doorListData.getString("doorStatus"));
                                            doorVO.setDoorTime(doorListData.getString("doorTime"));
                                            doorData.add(doorVO);
                                            logVO.setDoorList(doorData);
                                            Log.v(TAG,"JSON TEST doorListData=="+doorListData);
                                        }
                                        for(int i = 0; i < windowList.length(); i++){
                                            Log.v(TAG,"JSON TEST windowList i=="+i);
                                            WindowVO windowVO = new WindowVO();
                                            JSONObject windowListData = windowList.getJSONObject(i);
                                            windowVO.setWindowStatus(windowListData.getString("windowStatus"));
                                            windowVO.setWindowTime(windowListData.getString("windowTime"));
                                            windowData.add(windowVO);
                                            logVO.setWindowList(windowData);
                                            Log.v(TAG,"JSON TEST windowListData=="+windowListData);
                                        }
                                        for(int i = 0; i < lightList.length(); i++){
                                            Log.v(TAG,"JSON TEST  lightList i=="+i);
                                            LightVO lightVO = new LightVO();
                                            JSONObject lightListData = lightList.getJSONObject(i);
                                            lightVO.setLightStatus(lightListData.getString("lightStatus"));
                                            lightVO.setLightTime(lightListData.getString("lightTime"));
                                            lightData.add(lightVO);
                                            logVO.setLightList(lightData);
                                            Log.v(TAG,"JSON TEST lightListData=="+lightListData);
                                        }

                                    }
                                }
                            } catch (IOException e) {
                                Log.v(TAG, "IOException==" + e);
                            } catch (JSONException e){
                                Log.v(TAG, "JSONException==" + e);
                            }
                        }
                    }
                });
                thread1.start();
                while (true) {
                    String msg = sharedObject.pop();
                    printWriter.println(msg);
                    printWriter.flush();
                }
            } catch (IOException e) {
                Log.v(TAG, "Socket Communication IOException==" + e);
            }
        }
    });

    //음성인식 변화를 지속적으로 체크하여 실시간 반영되게 하는 Thread
    Thread voiceRecognitionCheck = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true) {
                appData = getSharedPreferences("appData", MODE_PRIVATE);
                voiceRecognition = appData.getBoolean("VOICE_RECOGNITION", false);
                editor = appData.edit();
                if(orderVoiceRecognition){
                    orderVoiceRecognition = false;
                    editor.putBoolean("VOICE_RECOGNITION", false);
                    editor.apply();
                }
            }
        }
    });


    //*************************** EventListener ***************************//
    /**
     * TabLayout SelectListenerEvent
     * Fragment Call
     */
    TabLayout.OnTabSelectedListener mTabSelect = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.v(TAG, "onTabSelected()_getPosition==" + tab.getPosition());
            fragmentTransaction = fragmentManager.beginTransaction();
            //FragmentWindow 에 TimePicker Component 가 swipeRefresh 떄문에 이벤트 터치가 겹처 작동하지 않아 해당 Fragment 에서는 비활성
            if (swipeRefresh.isEnabled() == false) {
                swipeRefresh.setEnabled(true);
            }
            switch (tab.getPosition()) {
                case 0:
                    if (fragmentHome == null) {
                        fragmentHome = new FragmentHome(sharedObject, bufferedReader, sensorDataVO);
                    }
                    fragmentTransaction.replace(
                            R.id.frame, fragmentHome).commitAllowingStateLoss();
                    fragmentHome.setArguments(bundle);
                    fragmentTag = 0;
                    break;
                case 1:
                    if (fragmentWindow == null) {
                        fragmentWindow = new FragmentWindow(sharedObject, bufferedReader, sensorDataVO, weatherVO);
                    }
                    fragmentTransaction.replace(
                            R.id.frame, fragmentWindow).commitAllowingStateLoss();
//                        fragmentWindow.setArguments(bundleFagmentA);
                    bundle.putSerializable("weather", weatherVO);
                    bundle.putSerializable("listFragmentWindow", listFragmentWindow);
                    bundle.putSerializable("sensorData", sensorDataVO);
                    fragmentWindow.setArguments(bundle);
                    tab.setIcon(R.drawable.toys_white_18dp);
//                    tab.setCustomView(createTabView(R.drawable.toys_white_18dp));
                    break;
                case 2:
                    Log.v(TAG, "onTabSelected()_speechRecognizer");
                    if (!dispatcher.isStopped()) {
                        dispatcher.stop();
                        pattenThread.interrupt();
                    }
                    speechRecognizer.startListening(intent);
                    break;
                case 3:
//                    if (fragmentAirConditioner == null) {
//                        fragmentAirConditioner = new FragmentAirConditioner(sharedObject, bufferedReader, this.sensorDataVO);
//                    }
                    fragmentTransaction.replace(
                            R.id.frame, fragmentAirConditioner).commitAllowingStateLoss();
                    bundle.putSerializable("weather", weatherVO);
                    bundle.putSerializable("sensorData", sensorDataVO);
                    fragmentAirConditioner.setArguments(bundle);
                    fragmentTag = 3;
                    break;
                case 4:
                    swipeRefresh.setEnabled(false);
                    if (fragmentSetting == null) {
                        fragmentSetting = new FragmentSetting(
                                sharedObject, bufferedReader, user);
                    }
                    fragmentTransaction.replace(
                            R.id.frame, fragmentSetting).commitAllowingStateLoss();
//                    bundle.putString("userEmail", user.getEmail());
                    bundle.putSerializable("LOGVO", logVO);
                    fragmentSetting.setArguments(bundle);
                    fragmentTag = 2;
//                    if (fragmentLight == null) {
//                        fragmentLight = new FragmentLight(sharedObject,bufferedReader);
//                    }
//                    fragmentTransaction.replace(
//                            R.id.frame, fragmentLight).commitAllowingStateLoss();
//                    fragmentTag = 4;
            }
        }

        //텝이 선택되지 않았을 때 호출
        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            Log.v(TAG, "onTabUnselected()_tab==" + tab.getPosition());
        }

        //텝이 다시 선택되었을 때 호출
        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            Log.v(TAG, "onTabReselected()_tab==" + tab.getPosition());
            switch (tab.getPosition()) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    Log.v(TAG, "onTabSelected()_speechRecognizer");
                    if (!dispatcher.isStopped()) {
                        dispatcher.stop();
                        pattenThread.interrupt();
                    }
                    speechRecognizer.startListening(intent);
                    break;
                case 3:
                    break;
                case 4:

            }
        }
    };

    /**
     * * Speech recognition
     */
    public void pattenRecognition(final Intent pattenIntent) {

        dispatcher =
                AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        double threshold = 5;
        double sensitivity = 45;
        final Handler handler = new Handler();
        final Runnable runn = new Runnable() {
            @Override
            public void run() {
                if (!dispatcher.isStopped()) {
                    dispatcher.stop();
                    pattenThread.interrupt();
                }
                speechRecognizer.startListening(pattenIntent);
            }
        };

        mPercussionDetector = new PercussionOnsetDetector(22050, 1024,
                new OnsetHandler() {
                    @Override
                    public void handleOnset(double time, double salience) {
                        if (voiceRecognition) {
                            Log.v(TAG, "time : " + time + ", salience : " + salience);
                            Log.v(TAG, "Clap detected!");
                            cntPatten++;
                            if (time - lastClapTime < 1 && time - lastClapTime > 0 && cntPatten >= 1) {
                                cntPatten = 0;
                                lastClapTime = 0;
                                handler.post(runn);
                            }
                            lastClapTime = time;
                        }
                    }

                }, sensitivity, threshold);

        dispatcher.addAudioProcessor(mPercussionDetector);
        pattenThread = new Thread(dispatcher, "Audio Dispatcher");
        pattenThread.start();
    }

    private RecognitionListener recognitionListener = new RecognitionListener() {
        String recogTAG = "RecognitionListener";

        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.v(recogTAG, "onReadyForSpeech()");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.v(recogTAG, "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float v) {
            Log.v(recogTAG, "onRmsChanged");
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            Log.v(recogTAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            Log.v(recogTAG, "onEndOfSpeech()");
            singleResult = true;
        }

        @Override
        public void onError(int i) {
            Log.v(recogTAG, "너무 늦게 말하면 오류뜹니다");
//            Toast.makeText(getApplicationContext(),"다시 말해",Toast.LENGTH_LONG);
//            speechRecognizer.startListening(intent);

//            Toast.makeText(getApplicationContext(),"다시 말해",Toast.LENGTH_LONG);
            //////////////////////////
            cntPatten = 0;
            lastClapTime = 0;
            pattenRecognition(intent);
            Log.v(TAG, "너무 늦게 말하면 오류뜹니다");

        }

        @Override
        public void onResults(Bundle bundle) {
            if(singleResult) {
                Log.v(TAG, "onResults");
                String key = "";
                key = SpeechRecognizer.RESULTS_RECOGNITION;
                ArrayList<String> mResult = bundle.getStringArrayList(key);

                String[] rs = new String[mResult.size()];
                mResult.toArray(rs);

                Log.v(TAG, "음성인식==" + rs[0]);
                Log.v(TAG, "음성인식size==" + mResult.size());
                String str = "";
                if (rs[0].contains("창문")) {
                    if (rs[0].contains("열어")) {
                        str = "창문을 열겠습니다";
                        speech(str);
                        sharedObject.put("/ANDROID>/WINDOW ON");
                    } else if (rs[0].contains("닫아")) {
                        str = "창문을 닫겠습니다";
                        speech(str);
                        sharedObject.put("/ANDROID>/WINDOW OFF");
                    }
                } else if (rs[0].contains("공기")) {
                    if (rs[0].contains("켜")) {
                        str = "공기청정기를 가동합니다";
                        speech(str);
                        sharedObject.put("/ANDROID>/AIRPURIFIER ON");
                    } else if (rs[0].contains("꺼")) {
                        str = "공기청정기 작동을 중지합니다";
                        speech(str);
                        sharedObject.put("/ANDROID>/AIRPURIFIER OFF");
                    }
                } else if (rs[0].contains("에어컨")) {
                    if (rs[0].contains("켜")) {
                        str = "에어컨을 가동합니다";
                        speech(str);
                        sharedObject.put("/ANDROID>/AIRCONDITIONER ON");
                    } else if (rs[0].contains("꺼")) {
                        str = "에어컨 작동을 중지합니다";
                        speech(str);
                        sharedObject.put("/ANDROID>/AIRCONDITIONER OFF");
                    }
                } else if (rs[0].contains("조용")) {
                    str = "음성감지를 중단합니다";
                    speech(str);
                    orderVoiceRecognition = true;
                }
                for (Fragment currentFragment : getSupportFragmentManager().getFragments()) {
                    if (currentFragment.isVisible()) {
                        if (currentFragment instanceof FragmentHome) {
                            Log.v(TAG, "FragmentHome");
                            startService(serviceIntent);
                        } else if (currentFragment instanceof FragmentWindow) {
                            fragmentTransaction = fragmentManager.beginTransaction();
                            if (fragmentWindow == null) {
                                fragmentWindow = new FragmentWindow(sharedObject, bufferedReader, sensorDataVO, weatherVO);
                            }
                            fragmentTransaction.replace(
                                    R.id.frame, fragmentWindow).commitAllowingStateLoss();
                            bundle.putSerializable("weather", weatherVO);
                            bundle.putSerializable("sensorData", sensorDataVO);
                            fragmentWindow.setArguments(bundle);
                            Log.v(TAG, "FragmentA_OnRefreshListener");
                        }
                    }
                }

                if (!dispatcher.isStopped()) {
                    dispatcher.stop();
                    pattenThread.interrupt();
                }
                pattenRecognition(intent);
                singleResult = false;
            }
        }

        @Override
        public void onPartialResults(Bundle bundle) {
        }

        @Override
        public void onEvent(int i, Bundle bundle) {
        }
    };

    /**
     * ReFreshListener
     */
    SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            /**
             * 가장 위에 표시된 Fragment 를 얻어와(getSupportFragmentManager().getFragments()) 해당 Fragment Refresh
             */
            Log.v(TAG, "onRefresh()_Fragment==" + getSupportFragmentManager().getFragments().toString());
            for (Fragment currentFragment : getSupportFragmentManager().getFragments()) {
                if (currentFragment.isVisible()) {
                    if (currentFragment instanceof FragmentHome) {
                        Log.v(TAG, "FragmentHome");
                        startService(serviceIntent);
                        break;
                    } else if (currentFragment instanceof FragmentWindow) {
                        fragmentTransaction = fragmentManager.beginTransaction();
                        if (fragmentWindow == null) {
                            fragmentWindow = new FragmentWindow(sharedObject, bufferedReader, sensorDataVO, weatherVO);
                        }
                        fragmentTransaction.replace(
                                R.id.frame, fragmentWindow).commitAllowingStateLoss();
                        bundle.putSerializable("weather", weatherVO);
                        bundle.putSerializable("sensorData", sensorDataVO);
                        fragmentWindow.setArguments(bundle);
                        Log.v(TAG, "FragmentA_OnRefreshListener");

                    } else if (currentFragment instanceof FragmentAirConditioner) {
                        Log.v(TAG, "FragmentAirConditioner");
                    } else if (currentFragment instanceof FragmentSetting) {
                        Log.v(TAG, "FragmentSetting");
                    } else if (currentFragment instanceof FragmentLight) {
                        Log.v(TAG, "FragmentLight");
                    }
                }
            }
            swipeRefresh.setRefreshing(false); //false 로 설정해야 새로고침 아이콘이 종료된다
            sharedObject.put("/ANDROID>/REFRESH ON");
        }
    };

    //실제 음성이 말하는 메소드
    private void speech(String msg) {
        tts.setPitch(1.5f); //1.5톤 올려서
        tts.setSpeechRate(1.0f); //1배속으로 읽기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
            // API 20
        else
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
    }
}
