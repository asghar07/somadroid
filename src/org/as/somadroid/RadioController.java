/*
        Developed by Andrea Stagi <http://4spills.blogspot.com/>

        Somadroid: a free SomaFM Client for Android phones (http://somafm.com/)
        Copyright (C) 2010 Andrea Stagi <http://4spills.blogspot.com/>

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/***
 * 
 * Module name: RadioController
 * Description: a class to control the radio
 * Date: 06/05/11
 * Author: Andrea Stagi <stagi.andrea(at)gmail.com>
 *
 ***/

package org.as.somadroid;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.Handler;

public class RadioController implements Controller {
    
    private Radio radio;
    private MediaPlayer player = new MediaPlayer();
    private ArrayList <RadioView> radioview = new ArrayList <RadioView>();
    Handler handler = new Handler();
    PrepareRadio prepare_radio = null;
    SomaActivity activity;

    
    public RadioController(Radio radio)
    {
        this.radio = radio;
    }
    
    public void play(SomaActivity activity, Channel ch)
    {
        this.radio.setChannel(ch);
        this.play(activity);
    }
    
    public void play(SomaActivity activity)
    {
        if(radio.getChannel() == null)
            return;
        radio.setIsPlaying(true);
        this.activity = activity;
        this.prepare_radio = new PrepareRadio(activity);
        this.prepareRadioHandler();   
        //RadioController.this.prepare_radio.execute();
    }
    
    private void prepareRadioHandler()
    {
        handler.removeMessages(0);
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if(!RadioController.this.prepare_radio.isCancelled())
                    RadioController.this.prepare_radio.cancel(true);
                RadioController.this.prepare_radio = new PrepareRadio(activity);
                RadioController.this.prepare_radio.execute();
            }
                 
         }, 100);
    }
    
    public void stop()
    {
        player.stop();
        radio.setIsPlaying(false);
        this.inform();
    }

    public void attach(RadioView obsEl) {
        this.radioview.add(obsEl);       
        obsEl.updateStatus(radio.isPlaying(), radio.getChannel());
    }

    public void detach(RadioView obsEl) {
        this.radioview.remove(obsEl);
    }

    @Override
    public void inform() {
        for(int i = 0; i < this.radioview.size(); i++)
            this.radioview.get(i).updateStatus(radio.isPlaying(), radio.getChannel());
    }

    public boolean isPlaying() {
        return this.radio.isPlaying();
    }

    
    public class PrepareRadio extends AsyncTask<Void,Void,Boolean > {
        
        private ProgressDialog dialog;  
        SomaActivity activity;
        
        public PrepareRadio(SomaActivity activity)
        {
            this.activity = activity;
        }
        
        @Override
        protected void onPreExecute() {
            
            activity.lockOrtientation();
            
            dialog = new ProgressDialog(activity);
            dialog.setMessage(activity.getString(R.string.buffering));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
            
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            player.release();
            player = MediaPlayer.create(Somadroid.app_context(), radio.getUri());
            
            if(player == null)
                return false;
            
            player.setOnPreparedListener(new OnPreparedListener() { 
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //mp.start();
                    radio.setIsPlaying(true);
                }
            });
            
            player.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer arg0, int arg1,
                        int arg2) {
                    radio.setIsPlaying(false);
                    return false;
                } 
            });
                
            return true;
         
        }

        protected void onPostExecute(Boolean result) {
            if(result != false)
            {
                player.start();
            }
            else
            {
                player = new MediaPlayer();
                radio.setIsPlaying(false);
            }
            
            RadioController.this.inform();
                
            try{
                dialog.dismiss();
            }catch(Exception e){}
            
            activity.unlockOrtientation();
        }
    }
    
}
