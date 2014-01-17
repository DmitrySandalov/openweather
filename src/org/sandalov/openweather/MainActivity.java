package org.sandalov.openweather;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private TextView cityField;
	private TextView dateField;
	private TextView temperatureField;
	private TextView temperatureUnitsField;
	private TextView humidityField;
	private TextView humidityUnitsField;
	private TextView pressureField;
	private TextView pressureUnitsField;
	private TextView windSpeedField;
	private TextView windSpeedUnitsField;
	private TextView conditionField;
	private LocationManager locationManager;
	private String provider;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		cityField = (TextView) findViewById(R.id.city);
		dateField = (TextView) findViewById(R.id.date);
		temperatureField = (TextView) findViewById(R.id.temperature);
		temperatureUnitsField = (TextView) findViewById(R.id.temperatureUnits);
		humidityField = (TextView) findViewById(R.id.humidity);
		humidityUnitsField = (TextView) findViewById(R.id.humidityUnits);
		pressureField = (TextView) findViewById(R.id.pressure);
		pressureUnitsField = (TextView) findViewById(R.id.pressureUnits);
		windSpeedField = (TextView) findViewById(R.id.windSpeed);
		windSpeedUnitsField = (TextView) findViewById(R.id.windSpeedUnits);
		conditionField = (TextView) findViewById(R.id.condition);

		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the location provider -> use
		// default
		Criteria criteria = new Criteria();
		provider = locationManager.getBestProvider(criteria, false);
		Location location = locationManager.getLastKnownLocation(provider);

		// Initialize the location fields
		if (location != null) {
			System.out.println("Provider " + provider + " has been selected.");
			onLocationChanged(location);
		} else {
			cityField.setText("Location not available");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_update:
	            updateWeather();
	            return true;
	        case R.id.action_settings:
	            // Launch settings activity
	            Intent i = new Intent(this, SettingsActivity.class);
	            startActivity(i);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	private void updateWeather() {
		Location location = locationManager.getLastKnownLocation(provider);
		// Initialize the location fields
		if (location != null) {
			System.out.println("Provider " + provider + " has been selected.");
			onLocationChanged(location);
		} else {
			cityField.setText("Location not available");
		}

		System.out.println("update!");
		
	}

	public void onLocationChanged(Location location) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String tempUnit = prefs.getString("units_temperature", null);

		String lat = String.valueOf(location.getLatitude());
		String lng = String.valueOf(location.getLongitude());
		GetJsonWeather weather = new GetJsonWeather();
		weather.execute(new String[]{lat, lng, tempUnit});
	}
	
	private class GetJsonWeather extends AsyncTask<String, Void, String> {

		private String cityName;
		private String cityCountry;
		private String temperature;
		private String temperatureUnits;
		private String humidity;
		private String humidityUnits;
		private String pressure;
		private String pressureUnits;
		private String windSpeed;
		private String windSpeedUnits;
		private String condition;
		private String date;		
		
		@Override
		protected String doInBackground(String... params) {
			
			// Get JSON
			DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());
			HttpGet httpget = new HttpGet("http://api.openweathermap.org/data/2.5/find?lat=" + params[0] +"&lon=" + params[1] + "&units=metric&type=accurate&cnt=1&lang=ru&mode=json");
			// Depends on your web service
			httpget.setHeader("Content-type", "application/json");

			InputStream inputStream = null;
			String result = null;
			try {
			    HttpResponse response = httpclient.execute(httpget);      
			    HttpEntity entity = response.getEntity();

			    inputStream = entity.getContent();
			    // json is UTF-8 by default
			    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
			    StringBuilder sb = new StringBuilder();

			    String line = null;
			    while ((line = reader.readLine()) != null)
			    {
			        sb.append(line + "\n");
			    }
			    result = sb.toString();
			} catch (Exception e) {
			    // Oops
			}
			finally {
			    try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
			}

			// Parse JSON
			try {
				JSONObject rootObj = new JSONObject(result);
				JSONArray list = rootObj.getJSONArray("list");
				for (int i=0; i<list.length(); i++) {
					JSONObject listObj = list.getJSONObject(i);
					cityName = listObj.getString("name");
					
					JSONObject sysObj = listObj.getJSONObject("sys");
					cityCountry = sysObj.getString("country");
					
					JSONObject mainObj = listObj.getJSONObject("main");
					int temp = mainObj.getInt("temp");
					if (params[2].equals(new String("Celsius"))) {
						temperature = String.valueOf(temp);
						temperatureUnits = getResources().getString(R.string.tempUnitsCelsius);
					} else {
						temperature = String.valueOf(temp * 9 / 5 + 32);
						temperatureUnits = getResources().getString(R.string.tempUnitsFarhenheit);
					}

					humidity = String.valueOf(mainObj.getInt("humidity"));
					humidityUnits = getResources().getString(R.string.unitsPercent);

					pressure = String.valueOf(mainObj.getInt("pressure"));
					pressureUnits = getResources().getString(R.string.pressureUnitsHpa);

					JSONObject windObj = listObj.getJSONObject("wind");
					windSpeed = String.valueOf(windObj.getInt("speed"));
					windSpeedUnits = getResources().getString(R.string.windSpeedUnitsMps);

					JSONObject weatherObj = listObj.getJSONArray("weather").getJSONObject(0);
					condition = weatherObj.getString("main");
				}				
			} catch (JSONException e) {
				e.printStackTrace();
			}

	    	SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss", Locale.US);
	    	date = df.format(Calendar.getInstance().getTime());

			return null;
		}

	    @Override
	    protected void onPostExecute(String result) {
			dateField.setText(date);
	    	cityField.setText(cityName + " (" + cityCountry + ")");
	    	temperatureField.setText(temperature);
	    	temperatureUnitsField.setText(temperatureUnits);
	    	humidityField.setText(humidity);
	    	humidityUnitsField.setText(humidityUnits);
	    	pressureField.setText(pressure);
	    	pressureUnitsField.setText(pressureUnits);
	    	windSpeedField.setText(windSpeed);
	    	windSpeedUnitsField.setText(windSpeedUnits);
	    	conditionField.setText(condition);

	    	Context context = getApplicationContext();
	    	CharSequence text = "Weather updated";
	    	int duration = Toast.LENGTH_SHORT;

	    	Toast toast = Toast.makeText(context, text, duration);
	    	toast.show();
	    }

	}

}
