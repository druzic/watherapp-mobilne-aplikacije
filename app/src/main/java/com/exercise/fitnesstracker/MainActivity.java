package com.exercise.fitnesstracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String API_KEY = "b69846c8fdb41a2c17a4ca89eb0e2e52";
    EditText city;
    TextView setWeather;
    TextView cityText; // Prikazuje za koji grad se prikazuje temperatura
    TextView setTemp;
    TextView FindCity;
    TextView feelsLike;
    TextView dayTemp;
    TextView nightTemp;
    TextView pressureCur;
    TextView humidityCur;
    LocationManager locationManager;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    public class DownloadWeather extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection httpURLConnection = null;
            try {
                url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(reader);
                String initString;
                while ((initString = br.readLine()) != null)
                    result += initString;
                in.close();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return "Failed";
            } finally {
                if (httpURLConnection != null)
                    httpURLConnection.disconnect();
            }
        }

        private String getApiKeyFromEnv() {
            Properties properties = new Properties();
            try {
                AssetManager assetManager = getApplicationContext().getAssets();
                InputStream inputStream = assetManager.open(".env");
                properties.load(inputStream);
                return properties.getProperty("API_KEY");
            } catch (IOException e) {
                Log.e("API_KEY", "Failed to read .env file: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.i("JSON:", s);
            if (s.equals("Failed")) {
                setWeather.setText("Failed To find!!");
            } else {
                String cityName = "";
                String clouds = "";
                try {
                    JSONObject json = new JSONObject(s);
                    JSONArray arr = new JSONArray(json.getString("weather"));
                    JSONObject jsontemp = json.getJSONObject("main");
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject jsonPart = arr.getJSONObject(i);
                        cityName += json.getString("name");
                        clouds += jsonPart.getString("description");
                    }
                    Double temp = jsontemp.getDouble("temp") - 273.15;
                    String formattedTemperature = String.format("%.1f", temp);
                    String formattedFellsLike = String.format("%.1f", jsontemp.getDouble("feels_like") - 273.15);
                    String Temp_max = String.format("%.1f", jsontemp.getDouble("temp_max") - 273.15);
                    String temp_min = String.format("%.1f", jsontemp.getDouble("temp_min") - 273.15);
                    String Pressure = String.format("%.1f", jsontemp.getDouble("pressure"));
                    String Humidity = String.format("%.1f", jsontemp.getDouble("humidity"));
                    setTemp.setText("Temp: " + formattedTemperature + "°C");
                    FindCity.setText("City: " + cityName );
                    feelsLike.setText("Osjet: " + formattedFellsLike + "°C");
                    dayTemp.setText("Dnevna: " + Temp_max + "°C");
                    nightTemp.setText("Noćna: " + temp_min + "°C");
                    pressureCur.setText("Tlak: " + Pressure + " mBar");
                    humidityCur.setText("Vlažnost: " + Humidity + "%");



                    // Dodajte kod za dobivanje vremenske prognoze za 7 dana ovdje
                    String city = json.getString("name");
                    get7DayWeather(city);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void getWeather(View view) {
        setWeather.setText("");
        if (city.getText().toString().isEmpty()) {
            // Provjeri je li dozvola za lokaciju već odobrena
            if (checkLocationPermission()) {
                // Dohvati trenutnu lokaciju
                getLocation();
            } else {
                // Zatraži dozvolu za lokaciju ako nije odobrena
                requestLocationPermission();
            }
        } else {
            // Ako je unesena lokacija, dohvatite podatke o vremenu za tu lokaciju
            if (checkLocationPermission()) {
                // Obriši ažuriranja lokacije kako ne biste koristili trenutnu lokaciju
                locationManager.removeUpdates(this);
            }
            String cityName = city.getText().toString();
            DownloadWeather task = new DownloadWeather();
            task.execute("https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&lang=hr&appid=" + API_KEY);
            InputMethodManager mr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            mr.hideSoftInputFromWindow(setWeather.getWindowToken(), 0);
        }
    }

    private void get7DayWeather(String city) {
        Log.d("Main Activity", city);
        String sevenDayUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + city + "&lang=hr&appid=" + API_KEY;
        DownloadWeather task = new DownloadWeather();
        task.execute(sevenDayUrl);
    }

    private void getLocation() {
        if (checkLocationPermission()) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    onLocationChanged(lastKnownLocation);
                }
            }
        } else {
            requestLocationPermission();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        city = findViewById(R.id.cityEditText);
        setWeather = findViewById(R.id.weatherTextView);
        cityText = findViewById(R.id.cityTextView);
        setTemp = findViewById(R.id.tempText);
        FindCity = findViewById(R.id.FindCity);
        feelsLike = findViewById(R.id.feelsLike);
        dayTemp = findViewById(R.id.dayTemp);
        nightTemp = findViewById(R.id.nightTemp);;
        pressureCur = findViewById(R.id.pressureCur);
        humidityCur = findViewById(R.id.humidityCur);


        // Provjera dozvole za pristup lokaciji
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Dohvati vremensku prognozu na temelju trenutne lokacije
        String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&lang=hr&appid=" + API_KEY;
        DownloadWeather task = new DownloadWeather();
        task.execute(weatherUrl);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkLocationPermission()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                onLocationChanged(lastKnownLocation);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    private boolean checkLocationPermission() {
        int permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Potrebna je dozvola za pristup lokaciji", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Dozvola odbijena", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

