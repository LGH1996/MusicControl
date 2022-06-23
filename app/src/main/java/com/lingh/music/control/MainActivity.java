package com.lingh.music.control;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.lingh.music.control.databinding.ActivityMainBinding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MyConfig myConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        myConfig = MyConfig.getInstance();
        setContentView(binding.getRoot());
        initViewState();
        initViewEvent();
        checkModuleState();
    }

    private void initViewState() {
        binding.scOnOff.setChecked(myConfig.isEnabled());
        binding.scOnlyEffectScreenOff.setChecked(myConfig.isOnlyEffectInScreenOff());
        binding.tvLongPressTime.setText(getString(R.string.long_press_time, myConfig.getLongPressTime()));
        binding.tvVibrateStrength.setText(getString(R.string.vibrate_strength, myConfig.getVibrateStrength()));
        binding.sbLongPressTime.setProgress(myConfig.getLongPressTime());
        binding.sbVibrateStrength.setProgress(myConfig.getVibrateStrength());
    }

    private void initViewEvent() {
        binding.scOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                myConfig.setEnabled(isChecked);
                myConfig.saveToFile();
                myConfig.notifyChange(MainActivity.this);
            }
        });
        binding.scOnlyEffectScreenOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                myConfig.setOnlyEffectInScreenOff(isChecked);
                myConfig.saveToFile();
                myConfig.notifyChange(MainActivity.this);
            }
        });
        binding.sbLongPressTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvLongPressTime.setText(getString(R.string.long_press_time, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                myConfig.setLongPressTime(seekBar.getProgress());
                myConfig.saveToFile();
                myConfig.notifyChange(MainActivity.this);
            }
        });
        binding.sbVibrateStrength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvVibrateStrength.setText(getString(R.string.vibrate_strength, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                myConfig.setVibrateStrength(seekBar.getProgress());
                myConfig.saveToFile();
                myConfig.notifyChange(MainActivity.this);
            }
        });
    }

    private void checkModuleState() {
        if (!isHookEnable()) {
            showModuleDisabledDialog();
        } else if (!isModuleValid()) {
            showModuleInvalidDialog();
        }
    }

    public boolean isModuleValid() {
        return false;
    }

    public boolean isHookEnable() {
        try {
            Class<?> classSystemProperties = Class.forName("android.os.SystemProperties");
            Method method = classSystemProperties.getDeclaredMethod("getBoolean", String.class, boolean.class);
            method.setAccessible(true);
            Object result = method.invoke(classSystemProperties, "persist.lingh.muco.enable", true);
            return (result == null || (boolean) result);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void showModuleDisabledDialog() {
        NormalDialog dialog = new NormalDialog();
        dialog.setCancelable(false);
        dialog.setShowMsg(getString(R.string.disabled_tip));
        dialog.setOnPositiveButtonClickListener("确定", null);
        dialog.show(getSupportFragmentManager(), null);
    }

    private void showModuleInvalidDialog() {
        NormalDialog dialog = new NormalDialog();
        dialog.setCancelable(false);
        dialog.setShowMsg(getString(R.string.invalid_tip));
        dialog.setOnPositiveButtonClickListener("确定", null);
        dialog.show(getSupportFragmentManager(), null);
    }

}