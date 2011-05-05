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
 * Module name: Somadroid
 * Date: 12/04/11
 * Author: Andrea Stagi <stagi.andrea(at)gmail.com>
 *
 ***/

package org.as.somadroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import org.as.somadroid.R;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class Somadroid extends ListActivity implements RadioView{
	
    private static boolean created = false;
    private PrepareAdapter pa = new PrepareAdapter(this, !created);
    private static Context context;
    private static SimpleAdapter current_adapter;
    
    private Menu mymenu = null;
    
    private boolean isplaying_menu;
    
    private static final Handler handler = new Handler();
    
    private BufferingDialog buffer_dialog;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        context = this.getApplicationContext();
        handler.removeMessages(0);
        setContentView(R.layout.custom_list_view);
        
        ((SomadroidApp) this.getApplication()).radio_controller.attach(this);
                
        if(created)   
            this.setAdapterAndNotify(current_adapter);
        
        pa.execute();
        created = true;
        this.buffer_dialog = new BufferingDialog(Somadroid.this);
        
    }
    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    
    void doTheAutoRefresh(long time) {
        handler.removeMessages(0);
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if(!pa.isCancelled())
                    pa.cancel(true);
                pa = new PrepareAdapter(Somadroid.this, false);
                pa.execute();
            }
                 
         }, time);
    }
    
    
    public SimpleAdapter getNewAdapter()
    {
    	
        if(!((SomadroidApp)this.getApplication()).channel_factory.createChannels())
            return null;
   		
        CopyOnWriteArrayList <Channel> chans2 = ((SomadroidApp)this.getApplication()).channel_factory.getChannels();  
                
        ArrayList<HashMap<String,Object>> list_populate = new ArrayList<HashMap<String,Object>>();   
	
        if(chans2 != null)
            list_populate = populateRadioList(chans2);

        SimpleAdapter adapter = new SimpleAdapter(Somadroid.this,list_populate,
                R.layout.custom_row_view,
                new String[] {"radio_logo", "radio_title","radio_listeners","radio_song"},
                new int[] { R.id.img, R.id.title,R.id.listeners, R.id.currentplay}
        ); 	
        
        current_adapter = adapter;
        return adapter;
    }
    
    
    public void setAdapterAndNotify(SimpleAdapter adapter)
    {
        this.setListAdapter(adapter);
        adapter.notifyDataSetChanged();
        ((SomadroidApp)this.getApplication()).channel_factory.inform();
    }
    

    public static Context app_context()
    {
        return context;
    }
    
    
    private ArrayList<HashMap<String,Object>>  populateRadioList(CopyOnWriteArrayList <Channel> arr) {
    	
        ArrayList<HashMap<String,Object>> list_populate = new ArrayList<HashMap<String,Object>>();   
    	
        for( int i = 0; i < arr.size(); i++ )
        {			
            HashMap<String,Object> temp = new HashMap<String,Object>();
            temp.put("radio_logo", arr.get(i).getImagePath());
            temp.put("radio_title",arr.get(i).getAttribute("title"));
            temp.put("radio_listeners", "Genre: " + arr.get(i).getAttribute("genre"));
            temp.put("radio_song", arr.get(i).getAttribute("lastPlaying"));
            temp.put("channel", arr.get(i));
            list_populate.add(temp);	
        }
        
        return list_populate;
    }
    
    
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        super.onListItemClick(l, v, position, id);
        HashMap o = (HashMap)this.getListAdapter().getItem(position);
        Channel ch = (Channel)o.get("channel");
        Intent intent = new Intent(this, PlayRadio.class);      
        
        ((SomadroidApp)this.getApplication()).channel_for_activity = ch;
        
        startActivity(intent);

    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.usermenu, menu);
        this.mymenu = menu;
        
        this.updateMenu();
        
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        
        switch (item.getItemId()) 
        {
            case R.id.stop:
                if(this.isplaying_menu)
                    ((SomadroidApp)this.getApplication()).radio_controller.stop();
                else
                {
                    buffer_dialog.start();
                    ((SomadroidApp)this.getApplication()).radio_controller.play();
                }
                return true;
            case R.id.about:
                return true;
            case R.id.exit:
                this.cleanExit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    protected void cleanExit() {
        ((SomadroidApp) this.getApplication()).radio_controller.stop();
        created = false;
        pa.cancel(true);
        handler.removeMessages(0);
        this.finish();
    }


    public void retryFeed() {
        pa = new PrepareAdapter(this, true);
        pa.execute();        
    }


    @Override
    public void updateStatus(boolean isPlaying, Channel currentCh) {    
        
        this.isplaying_menu = isPlaying;
        
        if(this.buffer_dialog != null)
            this.buffer_dialog.stop();
        
        if(this.mymenu == null)
            return;
        
        this.updateMenu();
        
    }
    
    
    public void updateMenu()
    {
        if(this.isplaying_menu)
            this.mymenu.getItem(0).setTitle("Pause");
        else
            this.mymenu.getItem(0).setTitle("Play");
    }
    
    
}