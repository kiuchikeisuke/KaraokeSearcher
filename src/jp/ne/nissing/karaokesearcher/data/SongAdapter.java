package jp.ne.nissing.karaokesearcher.data;

import java.util.List;

import jp.ne.nissing.karaokesearcher.R;
import android.content.Context;
import android.view.*;
import android.widget.*;

public class SongAdapter extends ArrayAdapter<Song>{

    private LayoutInflater mLayoutInflater;
    
    public SongAdapter(Context context, int resource, List<Song> objects) {
        super(context, resource, objects);
        
        this.mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
        final Song item = getItem(position);
        
        if(convertView == null){
            convertView = mLayoutInflater.inflate(R.layout.song_layout, null);
        }
        TextView songTitleView = (TextView) convertView.findViewById(R.id.text_view_song_title);
        songTitleView.setText(item.songTitle);
        
        TextView artistView = (TextView) convertView.findViewById(R.id.text_view_song_artist);
        artistView.setText(item.artistName);
        
        return convertView;
    }

}
