/* 
Copyright (c) 2017-2026, Vuzix Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

*  Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
    
*  Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
    
*  Neither the name of Vuzix Corporation nor the names of
   its contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.
    
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.vuzix.sample.vuzix_speech_recognition;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.util.List;

/**
 * Sample class to encapsulate all voice commands
 */
public class VoiceCmdReceiver  extends BroadcastReceiver {
    // Voice command substitutions. These substitutions are returned when phrases are recognized.
    // This is done by registering a phrase with a substitution. This eliminates localization issues
    // and is encouraged
    final String MATCH_POPUP = "popup";
    final String MATCH_CLEAR = "clear_substitution";
    final String MATCH_RESTORE = "restore";
    final String MATCH_EDIT_TEXT = "edit_text_pressed";

    // Voice command custom intent names
    final String TOAST_EVENT = "other_toast";

    private MainActivity mMainActivity;
    VuzixSpeechClient sc;
    boolean originalEnabledState;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerIntentFilter(IntentFilter intentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mMainActivity.registerReceiver(this, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            mMainActivity.registerReceiver(this, intentFilter);
        }
    }

    /**
     * <p>The speech SDK 1.8 and newer do <i>not</i> require this. That version was released in 2020 on
     * M400 v1.x products. If your code is only intended to run on current Vuzix hardware such as
     * M400, Blade2, Shield, and LX1, <u>you do not need</u> this functionality.</p>
     *
     * <p>This code us relevant only for M300 and original Blade.</p>
     *
     * <p>This mechanism is <u>strongly discouraged</u> on modern devices</p>
     */
    private void handleLegacyWakeWords() {
        // Delete every phrase in the dictionary! (Available in SDK version 1.3 and newer)
        //
        // Note! When developing applications on the Vuzix Blade and Vuzix M400, deleting all
        // phrases in the dictionary removes the wake-up word(s) and voice-off words. The M300
        // cannot change the wake-up word, so "hello vuzix" is unaffected by the deletePhrase call.
        sc.deletePhrase("*");

        // For Blade and M400, the wake-word can be modified. This call has no impact on the M300,
        // which ignores added/deleted wake words.
        //
        // Please always keep the wake word "hello vuzix" to prevent confusion. You can add additional
        // wake words specific to your application by imitating the code below
        try {
            sc.insertWakeWordPhrase("hello vuzix");      // Add-back the default phrase for consistency (Blade and M400 only)
        } catch (NoSuchMethodError e) {
            Log.i(mMainActivity.LOG_TAG, "Setting wake words is not supported. It is introduced in M300 v1.6.6, Blade v2.6, and M400 v1.0.0");
        }
        sc.insertWakeWordPhrase("hello sample app"); // Add application specific wake-up phrase (Blade and M400 only)

        try {
            // For all platforms, the voice-off phrase can be modified
            sc.insertVoiceOffPhrase("voice off");      // Add-back the default phrase for consistency
            sc.insertVoiceOffPhrase("privacy please"); // Add application specific stop listening phrase
            sc.insertVoiceOffPhrase("stop listening"); // Add application specific stop listening phrase
        } catch (NoSuchMethodError e) {
            Log.i(mMainActivity.LOG_TAG, "Setting voice off is not supported. It is introduced in M300 v1.6.6, Blade v2.6, and M400 v1.0.0");
        }
    }

    /**
     * Constructor which takes care of all speech recognizer registration
     * @param iActivity MainActivity from which we are created
     */
    public VoiceCmdReceiver(MainActivity iActivity)
    {
        mMainActivity = iActivity;
        registerIntentFilter(new IntentFilter(VuzixSpeechClient.ACTION_VOICE_COMMAND));
        Log.d(mMainActivity.LOG_TAG, "Connecting to Vuzix Speech SDK");
        try {
            float sdkVersion = 1.0f;
            try {
                sdkVersion = VuzixSpeechClient.getEngineVersion();
            } catch (Exception ignored) {
                Log.w(mMainActivity.LOG_TAG, "Unable to query speech SDK version running on the device");
            }

            // Create a VuzixSpeechClient from the SDK
            sc = new VuzixSpeechClient(iActivity);

            if (sdkVersion < 1.8f) {
                // This is not needed for most applications!
                // Most applications will exit with an error in this case.
                // This is for Vuzix devices with an Operating System prior to 2021.
                handleLegacyWakeWords();
            } else {
                // We want to delete the entire existing base vocabulary except the wake words and
                // voice-off words.
                // Rather than hard-coding those, we query the current words on the current system.
                // This returns the full list in the current language, and is very robust
                List<String> phrasesToKeep = sc.getWakeWordPhrases();
                phrasesToKeep.addAll( sc.getVoiceOffPhrases() );
                sc.deleteAllPhrasesExcept(phrasesToKeep);

                // NOTE: These should load string resources in a production application.
                // Hard-coded English phrases are used here for readability.
                sc.insertWakeWordPhrase("hello sample app"); // Add application specific wake-up phrase
                sc.insertVoiceOffPhrase("privacy please");
                sc.insertVoiceOffPhrase("stop listening");
            }

            // Now add any new strings.  If you put a substitution in the second argument, you will be passed that string instead of the full string
            // NOTE: These should load string resources in a production application.
            // Hard-coded English phrases are used here for readability.
            sc.insertKeycodePhrase("Alfa", KeyEvent.KEYCODE_A );
            sc.insertKeycodePhrase("Bravo", KeyEvent.KEYCODE_B);
            sc.insertKeycodePhrase("Charlie", KeyEvent.KEYCODE_C);
            sc.insertKeycodePhrase("Delta", KeyEvent.KEYCODE_D);
            sc.insertKeycodePhrase("Echo", KeyEvent.KEYCODE_E);
            sc.insertKeycodePhrase("Foxtrot", KeyEvent.KEYCODE_F);
            sc.insertKeycodePhrase("Golf", KeyEvent.KEYCODE_G);
            sc.insertKeycodePhrase("Hotel", KeyEvent.KEYCODE_H);
            sc.insertKeycodePhrase("India", KeyEvent.KEYCODE_I);
            sc.insertKeycodePhrase("Juliett", KeyEvent.KEYCODE_J);
            sc.insertKeycodePhrase("Kilo", KeyEvent.KEYCODE_K);
            sc.insertKeycodePhrase("Lima", KeyEvent.KEYCODE_L);
            sc.insertKeycodePhrase("Mike", KeyEvent.KEYCODE_M);
            sc.insertKeycodePhrase("November", KeyEvent.KEYCODE_N);
            sc.insertKeycodePhrase("Oscar", KeyEvent.KEYCODE_O);
            sc.insertKeycodePhrase("Papa", KeyEvent.KEYCODE_P);
            sc.insertKeycodePhrase("Quebec", KeyEvent.KEYCODE_Q);
            sc.insertKeycodePhrase("Romeo", KeyEvent.KEYCODE_R);
            sc.insertKeycodePhrase("Sierra", KeyEvent.KEYCODE_S);
            sc.insertKeycodePhrase("Tango", KeyEvent.KEYCODE_T);
            sc.insertKeycodePhrase("Uniform", KeyEvent.KEYCODE_U);
            sc.insertKeycodePhrase("Victor", KeyEvent.KEYCODE_V);
            sc.insertKeycodePhrase("Whiskey", KeyEvent.KEYCODE_W);
            sc.insertKeycodePhrase("X-Ray", KeyEvent.KEYCODE_X);
            sc.insertKeycodePhrase("Yankee", KeyEvent.KEYCODE_Y);
            sc.insertKeycodePhrase("Zulu", KeyEvent.KEYCODE_Z);
            // Misc
            sc.insertKeycodePhrase("Space", KeyEvent.KEYCODE_SPACE);
            sc.insertKeycodePhrase("shift", KeyEvent.KEYCODE_SHIFT_LEFT);
            sc.insertKeycodePhrase("caps lock", KeyEvent.KEYCODE_CAPS_LOCK);
            sc.insertKeycodePhrase("at sign", KeyEvent.KEYCODE_AT);
            sc.insertKeycodePhrase("period", KeyEvent.KEYCODE_PERIOD);
            sc.insertKeycodePhrase("erase", KeyEvent.KEYCODE_DEL);
            sc.insertKeycodePhrase("enter", KeyEvent.KEYCODE_ENTER);

            // Insert a custom intent.  Note: these are sent with sendBroadcastAsUser() from the service
            // If you are sending an event to another activity, be sure to test it from the adb shell
            // using: am broadcast -a "<your intent string>"
            // This example sends it to ourself, and we are sure we are active and registered for it
            Intent customToastIntent = new Intent(mMainActivity.CUSTOM_SDK_INTENT);
            sc.defineIntent(TOAST_EVENT, customToastIntent );
            sc.insertIntentPhrase("canned toast", TOAST_EVENT);

            // Insert phrases for our broadcast handler
            sc.insertPhrase(mMainActivity.getResources().getString(R.string.btn_text_pop_up),  MATCH_POPUP);
            // Since we have localized string resources, we demonstrate properly localing commands.
            // This is how a production app should do all insert() calls.
            sc.insertPhrase(mMainActivity.getResources().getString(R.string.btn_text_restore), MATCH_RESTORE);
            sc.insertPhrase(mMainActivity.getResources().getString(R.string.btn_text_clear),   MATCH_CLEAR);
            sc.insertPhrase("Edit Text", MATCH_EDIT_TEXT);

            // See what we've done
            Log.i(mMainActivity.LOG_TAG, sc.dump());

            // The recognizer may not yet be enabled in Settings. We can enable this directly.
            // Note: since this is system-wide, we will turn it off upon exiting
            originalEnabledState = VuzixSpeechClient.isRecognizerEnabled(mMainActivity);
            if(!originalEnabledState) {
                VuzixSpeechClient.EnableRecognizer(mMainActivity, true);
            }
        } catch(NoClassDefFoundError e) {
            // We get this exception if the SDK stubs against which we compiled cannot be resolved
            // at runtime. This occurs if the code is not being run on a Vuzix device supporting the voice
            // SDK
            Toast.makeText(iActivity, R.string.only_on_vuzix, Toast.LENGTH_LONG).show();
            Log.e(mMainActivity.LOG_TAG, iActivity.getResources().getString(R.string.only_on_vuzix), e );
            iActivity.finish();
        } catch (Exception e) {
            Log.e(mMainActivity.LOG_TAG, "Error setting custom vocabulary. ", e);
        }
    }

    /**
     * <p>All custom phrases registered with insertPhrase() are handled here.</p>
     *
     * <p>Custom intents may also be directed here, but this example does not demonstrate this.</p>
     *
     * <p>Keycodes are never handled via this interface</p>
     *
     * @param context Context in which the phrase is handled
     * @param intent Intent associated with the recognized phrase
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(mMainActivity.LOG_TAG, mMainActivity.getMethodName());
        // All phrases registered with insertPhrase() match ACTION_VOICE_COMMAND as do
        // recognizer status updates
        if (VuzixSpeechClient.ACTION_VOICE_COMMAND.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                // We will determine what type of message this is based upon the extras provided
                if (extras.containsKey(VuzixSpeechClient.PHRASE_STRING_EXTRA)) {
                    // If we get a phrase string extra, this was a recognized spoken phrase.
                    // The extra will contain the text that was recognized, unless a substitution
                    // was provided.  All phrases in this example have substitutions as it is
                    // considered best practice
                    String phrase = intent.getStringExtra(VuzixSpeechClient.PHRASE_STRING_EXTRA);
                    Log.e(mMainActivity.LOG_TAG, mMainActivity.getMethodName() + " \"" + phrase + "\"");
                    if(phrase == null) {
                        return;
                    }
                    // Determine the specific phrase that was recognized and act accordingly
                    switch (phrase) {
                        case MATCH_POPUP:
                            mMainActivity.OnPopupClick();
                            break;
                        case MATCH_RESTORE:
                            mMainActivity.OnRestoreClick();
                            break;
                        case MATCH_CLEAR:
                            mMainActivity.OnClearClick();
                            break;
                        case MATCH_EDIT_TEXT:
                            mMainActivity.SelectTextBox();
                            break;
                        default:
                            Log.e(mMainActivity.LOG_TAG, "Phrase not handled");
                            break;
                    }
                } else if (extras.containsKey(VuzixSpeechClient.RECOGNIZER_ACTIVE_BOOL_EXTRA)) {
                    // if we get a recognizer active bool extra, it means the recognizer was
                    // activated or stopped
                    boolean isRecognizerActive = extras.getBoolean(VuzixSpeechClient.RECOGNIZER_ACTIVE_BOOL_EXTRA, false);
                    mMainActivity.RecognizerChangeCallback(isRecognizerActive);
                } else {
                    Log.e(mMainActivity.LOG_TAG, "Voice Intent not handled");
                }
            }
        }
        else {
            Log.e(mMainActivity.LOG_TAG, "Other Intent not handled " + intent.getAction() );
        }
    }

    /**
     * Called to unregister for voice commands. An important cleanup step.
     */
    public void unregister() {
        try {
            // If the recognizer was disabled system-wide, restore that condition upon exiting
            VuzixSpeechClient.EnableRecognizer(mMainActivity, originalEnabledState);
            mMainActivity.unregisterReceiver(this);
            Log.i(mMainActivity.LOG_TAG, "Custom vocab removed");
            mMainActivity = null;
        }catch (Exception e) {
            Log.e(mMainActivity.LOG_TAG, "Custom vocab died " + e.getMessage());
        }
    }

    /**
     * Handler called when "Listen" button is clicked. Activates the speech recognizer identically to
     * saying "Hello Vuzix"
     *
     * @param bOnOrOff boolean True to enable listening, false to cancel it
     */
    public void TriggerRecognizerToListen(boolean bOnOrOff) {
        try {
            VuzixSpeechClient.TriggerVoiceAudio(mMainActivity, bOnOrOff);
        } catch (NoClassDefFoundError e) {
            // The voice SDK was added in version 1.2. The constructor will have failed if the
            // target device is not a Vuzix device that is compatible with SDK version 1.2.  But the
            // trigger command with the bool was added in SDK version 1.4.  It is possible the Vuzix
            // device does not yet have the TriggerVoiceAudio interface. If so, we get this exception.
            Toast.makeText(mMainActivity, R.string.upgrade_vuzix, Toast.LENGTH_LONG).show();
        }
    }
}
