package com.example.chattyapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {


    private RecyclerView chatsRV;
    private EditText userMsgEdit;
    private FloatingActionButton sendMsgFAB;
    private FloatingActionButton speakButton;
    private final String BOT_KEY = "bot";
    private final String USER_KEY = "user";
    private ArrayList<ChatsModal> chatsModalArrayList;
    private ChatRVAdapter chatRVAdapter;
    FirebaseTranslator spanishToEnglishTranslator;
    FirebaseTranslator englishToSpanishTranslator;
    String stringModal ="";
    TTSManager ttsManager= null;
    String speakStr = " No he dicho nada";
    private static final int REQ_CODE_SPEECH_INPUT=100;
    private FloatingActionButton listenB;
    private FloatingActionButton carB;
    ArrayList<String > Listen;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chatsRV= findViewById(R.id.idRVChats);
        listenB = findViewById(R.id.idListenB);
        userMsgEdit= findViewById(R.id.idEditMessage);
        sendMsgFAB= findViewById(R.id.idFABSend);
        speakButton = findViewById(R.id.idSpeakB);
        carB = findViewById(R.id.idPixiB);
        chatsModalArrayList= new ArrayList<>();

        chatRVAdapter = new ChatRVAdapter(chatsModalArrayList,this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        chatsRV.setLayoutManager(manager);
        chatsRV.setAdapter(chatRVAdapter);
        ttsManager= new TTSManager();
        ttsManager.init(this);

        // on below line we are creating our firebase translate option.
        FirebaseTranslatorOptions traductorParaEntrada =
                new FirebaseTranslatorOptions.Builder()
                        // below line we are specifying our source language.
                        .setSourceLanguage(FirebaseTranslateLanguage.ES)
                        // in below line we are displaying our target language.
                        .setTargetLanguage(FirebaseTranslateLanguage.EN)
                        // after that we are building our options.
                        .build();
        // below line is to get instance
        // for firebase natural language.
        spanishToEnglishTranslator = FirebaseNaturalLanguage.getInstance().getTranslator(traductorParaEntrada);
        FirebaseTranslatorOptions traductorParaSalida =
                new FirebaseTranslatorOptions.Builder()
                        // below line we are specifying our source language.
                        .setSourceLanguage(FirebaseTranslateLanguage.EN)
                        // in below line we are displaying our target language.
                        .setTargetLanguage(FirebaseTranslateLanguage.ES)
                        // after that we are building our options.
                        .build();
        // below line is to get instance
        // for firebase natural language.
        englishToSpanishTranslator= FirebaseNaturalLanguage.getInstance().getTranslator(traductorParaSalida);

        sendMsgFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userMsgEdit.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please Enter your Message", Toast.LENGTH_SHORT).show();
                    return;
                }
                // calling method to download language
                // modal to which we have to translate.
                String string = userMsgEdit.getText().toString();
                downloadModal(string);

            }
        });
        carB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iniciarAmbulancia(this);
            }
        });
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ttsManager.initQueue(speakStr);
            }
        });
        listenB.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                iniciarEntradaVoz();
            }
        });

    }
    private void iniciarAmbulancia(View.OnClickListener view){
        Intent intent = new Intent(this, ambulancePixi.class);
        startActivity(intent);

    }
    private void iniciarEntradaVoz(){
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hola, soy chatty");
        try{
            startActivityForResult(intent,REQ_CODE_SPEECH_INPUT);
        }catch (ActivityNotFoundException e){

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQ_CODE_SPEECH_INPUT:{
                if (resultCode==RESULT_OK && null!=data){
                    Listen=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    downloadModal(Listen.get(0));
                }
                break;
            }

        }
    }


    private void getResponse(String message){
        String url="http://api.brainshop.ai/get?bid=162097&key=TDkYnslkBKqaqJ1w&uid=[uid]&msg="+message;
        String BASE_url ="http://api.brainshop.ai/";
        Retrofit retrofit= new Retrofit.Builder().baseUrl(BASE_url).addConverterFactory(GsonConverterFactory.create()).build();

        RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);
        Call<MsgModel> call = retrofitAPI.getMessage(url);

        call.enqueue(new Callback<MsgModel>() {
            @Override
            public void onResponse(Call<MsgModel> call, Response<MsgModel> response) {
                if(response.isSuccessful()){
                    MsgModel modal = response.body();
                    stringModal = modal.getCnt();
                    downloadModalEnglishToSpanish(stringModal);
                }
            }

            @Override
            public void onFailure(Call<MsgModel> call, Throwable t) {
                chatsModalArrayList.add(new ChatsModal("Please reverb your question",BOT_KEY));
                chatRVAdapter.notifyDataSetChanged();
            }
        });{

        }
    }
    private void downloadModal(String input) {
        // below line is use to download the modal which
        // we will require to translate in german language
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();

        // below line is use to download our modal.
        spanishToEnglishTranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // calling method to translate our entered text.
                translateSpanishToEnglish(input);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Fail to download modal", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void downloadModalEnglishToSpanish (String input) {
        // below line is use to download the modal which
        // we will require to translate in german language
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();

        // below line is use to download our modal.
        englishToSpanishTranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // calling method to translate our entered text.
                translateEnglishToSpanish(input);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Fail to download modal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void translateSpanishToEnglish(String input) {
        chatsModalArrayList.add(new ChatsModal(input,USER_KEY));
        chatRVAdapter.notifyDataSetChanged();
        spanishToEnglishTranslator.translate(input).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                getResponse(s);
                userMsgEdit.setText("");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }
    private void translateEnglishToSpanish (String input) {
        englishToSpanishTranslator.translate(input).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                chatsModalArrayList.add(new ChatsModal(s,BOT_KEY));
                speakStr=s;
                chatRVAdapter.notifyDataSetChanged();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }
}