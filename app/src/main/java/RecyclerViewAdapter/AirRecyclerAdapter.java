package recyclerViewAdapter;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.semiproject.R;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Locale;

import communication.SharedObject;
import model.SensorDataVO;
import model.SystemInfoVO;
import model.WeatherVO;

public class AirRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    String TAG = "AirRecyclerAdapter";
    Context context;
    View view;
    private WeatherVO weatherVO;
    private SensorDataVO sensorDataVO;
    private BufferedReader bufferedReader;
    private SharedObject sharedObject;
    private ArrayList<SystemInfoVO> list;

    private boolean airPurifierSituation = false;
    private boolean windowSituation = false;
    private boolean airManualMode = false;
    private boolean airControlTouch = false;
    private boolean winControlTouch = false;

    SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();

    TextToSpeech tts;       //음석 출력관련 변수 선언

    int prePosition = -1;

    public AirRecyclerAdapter(Context context, SharedObject sharedObject, BufferedReader bufferedReader,
                              SensorDataVO sensorDataVO, WeatherVO weatherVO, ArrayList<SystemInfoVO> list) {
        this.context = context;
        this.bufferedReader = bufferedReader;
        this.sharedObject = sharedObject;
        this.sensorDataVO = sensorDataVO;
        this.weatherVO = weatherVO;
        this.list = list;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != android.speech.tts.TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (viewType == ViewType.ItemVerticalAir) {
            view = inflater.inflate(R.layout.recycler_item_airinfo, parent, false);
            return new AirRecyclerAdapter.AirInfo(view);
        } else {
            view = inflater.inflate(R.layout.recycler_item_systeminfo_air_control, parent, false);
            return new AirRecyclerAdapter.AirControl(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        Log.v(TAG, sensorDataVO.toString());
        if (holder instanceof AirInfo) {
            ((AirInfo) holder).tvPM25In.setText(sensorDataVO.getDust25() + " μg/m³");
            ((AirInfo) holder).tvPM10In.setText(sensorDataVO.getDust10() + " μg/m³");
            if(Integer.parseInt(sensorDataVO.getGasStatus()) < 900 && Integer.parseInt(sensorDataVO.getGasStatus()) >0){
                ((AirInfo) holder).tvGas.setText("양호");
                ((AirInfo) holder).tvGas.setTextColor(context.getResources().getColor(R.color.weatherColorGood, null));
            }else{
                ((AirInfo) holder).tvGas.setText("나쁨");
                ((AirInfo) holder).tvGas.setTextColor(context.getResources().getColor(R.color.weatherColorVeryBad, null));
            }
            ((AirInfo) holder).tvPM25Out.setText(weatherVO.getPm25Value() + " μg/m³");
            ((AirInfo) holder).tvPM10Out.setText(weatherVO.getPm10Value() + " μg/m³");
            // 공기상태 정보 표시 로직 끝
        } else if (holder instanceof AirControl) {
            if (sensorDataVO.getAirpurifierStatus().equals("1")) {
                ((AirControl) holder).swAirControl.setChecked(true);
            } else {
                ((AirControl) holder).swAirControl.setChecked(false);
            }
            if (sensorDataVO.getWindowStatus().equals("1")) {
                ((AirControl) holder).swWindowControl.setChecked(true);
            } else {
                ((AirControl) holder).swWindowControl.setChecked(false);
            }
            ((AirControl) holder).ivAirControl.setImageResource(R.drawable.ic_air_purifier);
            ((AirControl) holder).ivWindowControl.setImageResource(R.drawable.window2);
            ((AirControl) holder).tvAirControl.setText("공기청정기");
            ((AirControl) holder).tvWindowControl.setText("창문");
            ((AirControl) holder).swAirControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                    buttonView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isChecked) {
                                //공기청정기 Check
                                Log.v(TAG, "공기청정기 가동");
                                airPurifierSituation = true;
                                airManualMode = true;
                                airControlTouch = false;
                                sharedObject.put("/ANDROID>/AIRPURIFIER ON");
                                sharedObject.put("/ANDROID>/MODE OFF");
                            } else {
                                //공기청정기 unCheck
                                Log.v(TAG, "공기청정기 OFF");
                                airPurifierSituation = false;
                                airManualMode = true;
                                airControlTouch = false;
                                sharedObject.put("/ANDROID>/AIRPURIFIER OFF");
                                sharedObject.put("/ANDROID>/MODE OFF");
                            }
                        }
                    });
                }
            });

            ((AirControl) holder).swWindowControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                    buttonView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isChecked) {
                                Log.v(TAG, "창문 ON");
                                windowSituation = true;
                                airManualMode = true;
                                sharedObject.put("/ANDROID>/WINDOW ON");
                                sharedObject.put("/ANDROID>/MODE OFF");
                            } else {
                                //공기청정기 unCheck
                                Log.v(TAG, "창문 OFF");
                                windowSituation = false;
                                airManualMode = true;
                                sharedObject.put("/ANDROID>/WINDOW OFF");
                                sharedObject.put("/ANDROID>/MODE OFF");
                            }
                        }
                    });
                }
            });
            if(sensorDataVO.getAirpurifierStatus().equals("ON")){
                ((AirControl) holder).swAirControl.setChecked(true);
            }else{
                ((AirControl) holder).swAirControl.setChecked(false);
            }
            if(sensorDataVO.getWindowStatus().equals("ON")){
                ((AirControl) holder).swWindowControl.setChecked(true);
            }else{
                ((AirControl) holder).swWindowControl.setChecked(false);
            }

            // 공기청정기, 창문 컨트롤 로직설정 끝
        }


//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.v(TAG, "onBindViewHolder()_onClick()_position==" + position);
//                switch (position) {
//                    case 0:
//                        Log.v(TAG, "onBindViewHolder()_onClick()_position=0=" + position);
//                        break;
//                    case 1:
//                        Log.v(TAG, "onBindViewHolder()_onClick()_position=1=" + position);
//                        break;
//                    case 3:
//                        if (sparseBooleanArray.get(position)) {
//                            // 펼쳐진 Item을 클릭 시
//                            sparseBooleanArray.delete(position);
//                        } else {
//                            // 직전의 클릭됐던 Item의 클릭상태를 지움
//                            sparseBooleanArray.delete(prePosition);
//                            // 클릭한 Item의 position을 저장
//                            sparseBooleanArray.put(position, true);
//                        }
//                        // 해당 포지션의 변화를 알림
//                        if (prePosition != -1) notifyItemChanged(prePosition);
//                        notifyItemChanged(position);
//                        // 클릭된 position 저장
//                        prePosition = position;
//                        break;
//                }
//            }
//        });
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).getViewType();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void speech(String msg) {
        tts.setPitch(1.5f); //1.5톤 올려서
        tts.setSpeechRate(1.0f); //1배속으로 읽기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
            // API 20
        else
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
    }


    public class AirInfo extends RecyclerView.ViewHolder {
        TextView tvPM25In;
        TextView tvPM10In;
        TextView tvGas;

        TextView tvPM25Out;
        TextView tvPM10Out;

        public AirInfo(@NonNull View itemView) {
            super(itemView);
            tvPM25In = itemView.findViewById(R.id.tvPM25In);
            tvPM10In = itemView.findViewById(R.id.tvPM10In);
            tvGas = itemView.findViewById(R.id.tvGas);
            tvPM25Out = itemView.findViewById(R.id.tvPM25Out);
            tvPM10Out = itemView.findViewById(R.id.tvPM10Out);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.v(TAG, "클릭되었음"+getAdapterPosition());

                }
            });

        }
    }

    public class AirControl extends RecyclerView.ViewHolder {
        ImageView ivAirControl;
        ImageView ivWindowControl;
        TextView tvAirControl;
        TextView tvWindowControl;
        Switch swAirControl;
        Switch swWindowControl;

        public AirControl(@NonNull View itemView) {
            super(itemView);
            ivAirControl = itemView.findViewById(R.id.ivAirControl);
            ivWindowControl = itemView.findViewById(R.id.ivWindowControl);
            tvAirControl = itemView.findViewById(R.id.tvAirControl);
            tvWindowControl = itemView.findViewById(R.id.tvWindowControl);
            swAirControl = itemView.findViewById(R.id.swAirControl);
            swWindowControl = itemView.findViewById(R.id.swWindowControl);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.v(TAG, "클릭되었음"+getAdapterPosition());

                }
            });
        }
    }
}
