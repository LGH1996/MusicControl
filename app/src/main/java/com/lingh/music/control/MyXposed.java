package com.lingh.music.control;

import android.app.AndroidAppHelper;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MyXposed implements IXposedHookLoadPackage {
    private static final Class<?> classSystemProperties = XposedHelpers.findClass("android.os.SystemProperties", MyXposed.class.getClassLoader());
    private static final boolean isHookEnable = (boolean) XposedHelpers.callStaticMethod(classSystemProperties, "getBoolean", "persist.lingh.muco.enable", true);
    private MyConfig myConfig = MyConfig.getInstance();

    {
        XposedBridge.log("LinGH load config" + myConfig);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (!isHookEnable) {
            return;
        }

        if (TextUtils.equals(lpparam.packageName, "com.lingh.music.control")) {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.lingh.music.control.MainActivity", lpparam.classLoader), "isModuleValid", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }

        if (TextUtils.equals(lpparam.packageName, "android") && TextUtils.equals(lpparam.processName, "android")) {

            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader), "systemBooted", new XC_MethodHook() {

                private static final String ACTION_UPDATE_CONFIG = "action.com.lingh.music.control.update.config";
                private BroadcastReceiver broadcastReceiver;

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (broadcastReceiver != null) {
                            return;
                        }
                        broadcastReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                try {
                                    String config = intent.getStringExtra("config");
                                    if (config != null) {
                                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                        myConfig = gson.fromJson(config, MyConfig.class);
                                    }
                                    XposedBridge.log("LinGH update config " + myConfig);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        AndroidAppHelper.currentApplication().registerReceiver(broadcastReceiver, new IntentFilter(ACTION_UPDATE_CONFIG));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });

            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", lpparam.classLoader), "interceptKeyBeforeQueueing", new XC_MethodHook() {

                private volatile boolean isTowPress;
                private volatile boolean isUpKeyPress, isDownKeyPress;
                private volatile long upKeyPressTime, downKeyPressTime;
                private volatile boolean isChangeVolume;
                private AudioManager audioManager;
                private Vibrator vibrator;
                private PowerManager powerManager;
                private ScheduledFuture<?> future;
                private ScheduledExecutorService executorService;
                private Instrumentation instrumentation;


                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (XposedHelpers.getBooleanField(param.thisObject, "mSystemBooted")) {

                            if (audioManager == null) {
                                audioManager = AndroidAppHelper.currentApplication().getSystemService(AudioManager.class);
                            }
                            if (vibrator == null) {
                                vibrator = AndroidAppHelper.currentApplication().getSystemService(Vibrator.class);
                            }
                            if (powerManager == null) {
                                powerManager = AndroidAppHelper.currentApplication().getSystemService(PowerManager.class);
                            }
                            if (executorService == null) {
                                executorService = Executors.newSingleThreadScheduledExecutor();
                            }
                            if (instrumentation == null) {
                                instrumentation = new Instrumentation();
                            }

                            if (!myConfig.isEnabled()) {
                                return;
                            }

                            if (myConfig.isOnlyEffectInScreenOff() && powerManager.isInteractive()) {
                                return;
                            }

                            if (!KeyEvent.class.isAssignableFrom(param.args[0].getClass())) {
                                return;
                            }

                            KeyEvent event = (KeyEvent) param.args[0];

                            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                                if (isChangeVolume) {
                                    if (event.getAction() == KeyEvent.ACTION_UP) {
                                        isChangeVolume = false;
                                    }
                                    return;
                                }
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    upKeyPressTime = event.getEventTime();
                                    isUpKeyPress = true;
                                    isTowPress = false;
                                    if (!isDownKeyPress) {
                                        future = executorService.schedule(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isDownKeyPress) {
                                                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                                                    vibrator.vibrate(myConfig.getVibrateStrength() / 20);
                                                    XposedBridge.log("LinGH music play-pause");
                                                } else if (isUpKeyPress && audioManager.isMusicActive()) {
                                                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_NEXT);
                                                    vibrator.vibrate(myConfig.getVibrateStrength() / 20);
                                                    XposedBridge.log("LinGH music next");
                                                }
                                            }
                                        }, myConfig.getLongPressTime(), TimeUnit.MILLISECONDS);
                                    } else {
                                        isTowPress = true;
                                    }
                                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                                    future.cancel(false);
                                    isUpKeyPress = false;
                                    if (!isTowPress && event.getEventTime() - upKeyPressTime < myConfig.getLongPressTime()) {
                                        isChangeVolume = true;
                                        executorService.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP);
                                                XposedBridge.log("LinGH volume up");
                                            }
                                        });
                                    }
                                }
                                param.setResult(0);
                            }

                            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                                if (isChangeVolume) {
                                    if (event.getAction() == KeyEvent.ACTION_UP) {
                                        isChangeVolume = false;
                                    }
                                    return;
                                }
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    downKeyPressTime = event.getEventTime();
                                    isDownKeyPress = true;
                                    isTowPress = false;
                                    if (!isUpKeyPress) {
                                        future = executorService.schedule(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isUpKeyPress) {
                                                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                                                    vibrator.vibrate(myConfig.getVibrateStrength() / 20);
                                                    XposedBridge.log("LinGH music play-pause");
                                                } else if (isDownKeyPress && audioManager.isMusicActive()) {
                                                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                                                    vibrator.vibrate(myConfig.getVibrateStrength() / 20);
                                                    XposedBridge.log("LinGH music previous");
                                                }
                                            }
                                        }, myConfig.getLongPressTime(), TimeUnit.MILLISECONDS);
                                    } else {
                                        isTowPress = true;
                                    }
                                    param.setResult(0);
                                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                                    future.cancel(false);
                                    isDownKeyPress = false;
                                    if (!isTowPress && event.getEventTime() - downKeyPressTime < myConfig.getLongPressTime()) {
                                        isChangeVolume = true;
                                        executorService.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_DOWN);
                                                XposedBridge.log("LinGH volume down");
                                            }
                                        });
                                    }
                                }
                                param.setResult(0);
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
