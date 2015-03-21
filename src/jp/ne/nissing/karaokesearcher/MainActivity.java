package jp.ne.nissing.karaokesearcher;

import java.io.IOException;
import java.util.*;

import jp.ne.nissing.karaokesearcher.data.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.*;

import android.app.*;
import android.content.Context;
import android.os.*;
import android.view.*;
import android.view.View.OnTouchListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SearchView.OnQueryTextListener;

public class MainActivity extends Activity {

    private Context mContext = null;
    private SongAdapter mSongAdapter = null;
    private ProgressDialog mProgressDialog = null;
    
    /**
     * 検索用のURI
     */
    private final String REQUEST_URI = "http://ec2-54-92-60-143.ap-northeast-1.compute.amazonaws.com/search?";
    /**
     * Mock用のURI
     */
    //private final String REQUEST_URI = "http://private-anon-45e438783-karaokesearch.apiary-mock.com/search?";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        init();
    }

    private void init() {
        
        //searchViewの設定
        final SearchView searchView = (SearchView) this.findViewById(R.id.search_view_music_searcher);
        searchView.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                searchView.setIconified(false);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            
            @Override
            public boolean onQueryTextSubmit(String query) {
                SearchQueryAsyncTask search = new SearchQueryAsyncTask();
                search.execute(new String[]{query});
                return false;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        
        //listViewの設定
        ListView searchResultListView = (ListView) findViewById(R.id.list_view_search_result);
        mSongAdapter = new SongAdapter(this, 0, new ArrayList<Song>());
        searchResultListView.setAdapter(mSongAdapter);
        searchResultListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
            }
        });
    }
    
    private class SearchResult{
        public static final int RESULT_OK = 0;
        public static final int RESULT_SEARCH_ERROR = 1;
        public static final int RESULT_QUERY_ERROR = 2;
        public int ResultCode;
        public String result;
        
        public SearchResult(int resultCode,String result){
            this.ResultCode =resultCode;
            this.result = result;
        }
    }
    private class SearchQueryAsyncTask extends AsyncTask<String, Integer, SearchResult>{

        
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setProgress(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage(getResources().getString(R.string.progress_dialog_searching));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }
        
        @Override
        protected SearchResult doInBackground(String... params) {
            if(params.length != 1){
                return new SearchResult(SearchResult.RESULT_QUERY_ERROR, null);
            }
            SearchResult result = search(params[0]);
            return result;
        }
        
        private SearchResult search(String query){
            String requestUriString = createRequestSearchQuery(query);
            
            if(requestUriString == null){
                return new SearchResult(SearchResult.RESULT_QUERY_ERROR, null);
            }
            
            //HttpClientでリクエストを投げる
            HttpGet getRequest = new HttpGet(requestUriString);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            String result = null;
            try {
                result = httpClient.execute(getRequest, new ResponseHandler<String>() {
                    
                    @Override
                    public String handleResponse(HttpResponse response)
                            throws ClientProtocolException, IOException {
                        String retval = null;
                        switch (response.getStatusLine().getStatusCode()) {
                        case HttpStatus.SC_OK:
                            retval = EntityUtils.toString(response.getEntity(),"UTF-8");
                        case HttpStatus.SC_BAD_REQUEST:
                        default:
                            break;
                        }
                        return retval;
                    }
                });
                if(result != null){
                    return new SearchResult(SearchResult.RESULT_OK, result);
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new SearchResult(SearchResult.RESULT_SEARCH_ERROR, null);
        }
        
        @Override
        protected void onPostExecute(SearchResult searchResult) {
            super.onPostExecute(searchResult);
            mProgressDialog.dismiss();
            
            //エラー処理
            if(searchResult.ResultCode == SearchResult.RESULT_QUERY_ERROR){
                Toast.makeText(mContext, R.string.toast_empty_query, Toast.LENGTH_LONG).show();
                return;
            }
            else if(searchResult.ResultCode == SearchResult.RESULT_SEARCH_ERROR){
                Toast.makeText(mContext, R.string.toast_search_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            
            
            //ListViewの表示/非表示切り替え
            ListView listView = (ListView) findViewById(R.id.list_view_search_result);
            if(listView.getVisibility() == View.GONE){
                listView.setVisibility(View.VISIBLE);
                findViewById(R.id.text_view_no_search).setVisibility(View.GONE);
            }
            
            //正しく結果が取得された場合はリストを再作成
            try {
                mSongAdapter.clear();
                JSONArray resultJsonArray = new JSONArray(searchResult.result);
                List<Song> songs = new ArrayList<Song>();
                for(int i = 0; i < resultJsonArray.length();i++){
                    JSONObject songObj = resultJsonArray.getJSONObject(i);
                    Song song = new Song();
                    song.id = songObj.getInt("id");
                    song.songId = songObj.getString("song_id");
                    song.artistId = songObj.getString("artist_id");
                    song.songTitle = songObj.getString("song_title");
                    song.songTitleSearch = songObj.getString("song_title_search");
                    song.artistName = songObj.getString("artist_name");
                    song.artistNameSearch = songObj.getString("artist_name_search");
                    song.createdAt = songObj.getString("created_at");
                    song.updatedAt = songObj.getString("updated_at");
                    
                    songs.add(song);
                }
                mSongAdapter.addAll(songs);
                Toast.makeText(mContext,getString(R.string.toast_search_result,songs.size()) , Toast.LENGTH_SHORT).show();
                
            } catch (JSONException e) {
                e.printStackTrace();
            }
            
        }
    }
    
    
    private String createRequestSearchQuery(String query){
        String retval = null;
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_group_mode_selecter);
        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        
        if(checkedRadioButtonId == R.id.radio_button_title_select){
            retval = REQUEST_URI + "title=" + query;
        }else if(checkedRadioButtonId == R.id.radio_button_artist_select){
            retval = REQUEST_URI + "artist="+ query;
        }else if(checkedRadioButtonId == R.id.radio_button_title_artist_select){
            String[] queries = query.split("\\s");
            if(queries.length > 1){
                retval = REQUEST_URI + "artist=" + queries[0] + "&title=" + queries[1];
            }
        }else{
            //特に何もしない
        }
        return retval;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
