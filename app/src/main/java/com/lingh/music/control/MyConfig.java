package com.lingh.music.control;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class MyConfig {
    private static final String PATH = "//data//user//0//com.lingh.music.control//config.ini";
    private static final String ACTION_UPDATE_CONFIG = "action.com.lingh.music.control.update.config";
    private static MyConfig instance;
    private boolean enabled;
    private boolean onlyEffectInScreenOff;
    private int longPressTime;
    private int vibrateStrength;

    private MyConfig(boolean enabled, boolean onlyEffectInScreenOff, int longPressTime, int vibrateStrength) {
        this.enabled = enabled;
        this.onlyEffectInScreenOff = onlyEffectInScreenOff;
        this.longPressTime = longPressTime;
        this.vibrateStrength = vibrateStrength;
    }

    public static MyConfig getInstance() {
        if (instance == null) {
            try (Scanner scanner = new Scanner(new File(PATH))) {
                StringBuilder builder = new StringBuilder();
                while (scanner.hasNextLine()) {
                    builder.append(scanner.nextLine());
                }
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                instance = gson.fromJson(builder.toString(), MyConfig.class);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (instance == null) {
            instance = new MyConfig(true, false, 800, 1000);
        }
        return instance;
    }

    public void saveToFile() {
        try (FileOutputStream outPutStream = new FileOutputStream(PATH)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String str = gson.toJson(this, MyConfig.class);
            outPutStream.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void notifyChange(Context context) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String str = gson.toJson(this, MyConfig.class);
        Intent intent = new Intent(ACTION_UPDATE_CONFIG);
        intent.setPackage("android");
        intent.putExtra("config", str);
        context.sendBroadcast(intent);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOnlyEffectInScreenOff() {
        return onlyEffectInScreenOff;
    }

    public void setOnlyEffectInScreenOff(boolean onlyEffectInScreenOff) {
        this.onlyEffectInScreenOff = onlyEffectInScreenOff;
    }

    public int getLongPressTime() {
        return longPressTime;
    }

    public void setLongPressTime(int longPressTime) {
        this.longPressTime = longPressTime;
    }

    public int getVibrateStrength() {
        return vibrateStrength;
    }

    public void setVibrateStrength(int vibrateStrength) {
        this.vibrateStrength = vibrateStrength;
    }

    @Override
    public String toString() {
        return "MyConfig{" +
                "enabled=" + enabled +
                ", onlyEffectInScreenOff=" + onlyEffectInScreenOff +
                ", longPressTime=" + longPressTime +
                ", vibrateStrength=" + vibrateStrength +
                '}';
    }
}
