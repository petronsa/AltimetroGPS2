package com.app.petron.altimetrogps;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;


import com.startapp.android.publish.ads.nativead.NativeAdDetails;
import com.startapp.android.publish.ads.nativead.NativeAdPreferences;
import com.startapp.android.publish.ads.nativead.StartAppNativeAd;
import com.startapp.android.publish.adsCommon.Ad;
import com.startapp.android.publish.adsCommon.StartAppAd;
import com.startapp.android.publish.adsCommon.StartAppAd.AdMode;
import com.startapp.android.publish.adsCommon.StartAppSDK;
import com.startapp.android.publish.adsCommon.VideoListener;
import com.startapp.android.publish.adsCommon.adListeners.AdDisplayListener;
import com.startapp.android.publish.adsCommon.adListeners.AdEventListener;



public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener{
    public double opcion;
    private StartAppAd startAppAd = new StartAppAd(this);
    private static final String LOGTAG = "android-localizacion";

    private static final int PETICION_PERMISO_LOCALIZACION = 101;
    private static final int PETICION_CONFIG_UBICACION = 201;

    private GoogleApiClient apiClient;

    private TextView lblLatitud;
    private TextView lblLongitud;
    private TextView lblAltitud;
    private ToggleButton btnActualizar;
    private LocationRequest locRequest;
    private Switch switch1;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StartAppSDK.init(this, "101423750", "210832874", true);
        setContentView(R.layout.activity_main);
        lblLatitud = (TextView) findViewById(R.id.lblLatitud);
        lblLongitud = (TextView) findViewById(R.id.lblLongitud);
        lblAltitud = (TextView) findViewById(R.id.lblAltitud);
        btnActualizar = (ToggleButton) findViewById(R.id.btnActualizar);
        btnActualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleLocationUpdates(btnActualizar.isChecked());
                guardarpreferencias();
            }
        });
        //Construcción cliente API Google
        apiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        switch1 = (Switch) findViewById(R.id.switch1);

        cargarpreferencias();

        //poner el swicth a escuchar
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isCkecked) {

                if(isCkecked){
                    opcion = 3.28;
                }else {
                    opcion = 1.00;
                }
            }
        });

        if(switch1.isChecked()){
            opcion = 3.28;
        }
        else{
            opcion = 1.00;
        }


        startAppAd.onResume();


    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();

        guardarpreferencias();

        //poner en marcha la publicidad al salir
        MainActivity.this.startAppAd.showAd();
        MainActivity.this.startAppAd.loadAd();

    }


    @Override
    public void onPause() {
        super.onPause();
        startAppAd.onPause();
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

        public void cargarpreferencias(){
            SharedPreferences preferences = getSharedPreferences("Preferencias",Context.MODE_PRIVATE);
            switch1.setChecked(preferences.getBoolean("checked",false));
        }

        public void guardarpreferencias (){
            SharedPreferences preferences = getSharedPreferences("Preferencias",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor=preferences.edit();
            boolean valor = switch1.isChecked();
            editor.putBoolean("checked",valor);
            editor.commit();

      }





    private void toggleLocationUpdates(boolean enable) {
        if (enable) {
            enableLocationUpdates();
        } else {
            disableLocationUpdates();
        }
    }

    private void enableLocationUpdates() {

        locRequest = new LocationRequest();
        locRequest.setInterval(2000);
        locRequest.setFastestInterval(1000);
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest locSettingsRequest =
                new LocationSettingsRequest.Builder()
                        .addLocationRequest(locRequest)
                        .build();

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        apiClient, locSettingsRequest);

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        Log.i(LOGTAG, "Configuración correcta");
                        startLocationUpdates();

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            Log.i(LOGTAG, "Se requiere actuación del usuario");
                            status.startResolutionForResult(MainActivity.this, PETICION_CONFIG_UBICACION);
                        } catch (IntentSender.SendIntentException e) {
                            btnActualizar.setChecked(false);
                            Log.i(LOGTAG, "Error al intentar solucionar configuración de ubicación");
                        }

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(LOGTAG, "No se puede cumplir la configuración de ubicación necesaria");
                        btnActualizar.setChecked(false);
                        break;
                }
            }
        });
    }

    private void disableLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(
                apiClient, this);

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            //Ojo: estamos suponiendo que ya tenemos concedido el permiso.
            //Sería recomendable implementar la posible petición en caso de no tenerlo.

            Log.i(LOGTAG, "Inicio de recepción de ubicaciones");

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    apiClient, locRequest, MainActivity.this);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        //Se ha producido un error que no se puede resolver automáticamente
        //y la conexión con los Google Play Services no se ha establecido.

        Log.e(LOGTAG, "Error grave al conectar con Google Play Services");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Conectado correctamente a Google Play Services

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PETICION_PERMISO_LOCALIZACION);
        } else {

            Location lastLocation =
                    LocationServices.FusedLocationApi.getLastLocation(apiClient);

            updateUI(lastLocation);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Se ha interrumpido la conexión con Google Play Services

        Log.e(LOGTAG, "Se ha interrumpido la conexión con Google Play Services");
    }

    private void updateUI(Location loc) {



        if (loc != null) {
            String conversor,conversor3;
            //convertimos la altitud en una variable String
            conversor = String.valueOf(loc.getAltitude());
            //
            Double conversor1,conversor2;
            //convertimos la altura de nuevo a Double para poder convertirla
            conversor1 = Double.valueOf(conversor);
            //ahora convertimos en metros o pies depende de opcion
            conversor2 = conversor1*opcion;
            //Redondeamos la altura a un decimal y se convierte en String
            conversor3 = String.format("%.1f",conversor2);



            //utilizamos getText para entrar el R.string más los valores

            lblLatitud.setText(getText(R.string.latitud) + String.valueOf(loc.getLatitude()));
            lblLongitud.setText(getText(R.string.longitud) + String.valueOf(loc.getLongitude()));
            lblAltitud.setText(getText(R.string.altitud)+ conversor3);
        } else {
            lblLatitud.setText(R.string.latitud_desconocida);
            lblLongitud.setText(R.string.longitud_desconocida);
            lblAltitud.setText(R.string.altitud_desconocida);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PETICION_PERMISO_LOCALIZACION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //Permiso concedido

                @SuppressWarnings("MissingPermission")
                Location lastLocation =
                        LocationServices.FusedLocationApi.getLastLocation(apiClient);

                updateUI(lastLocation);

            } else {
                //Permiso denegado:
                //Deberíamos deshabilitar toda la funcionalidad relativa a la localización.

                Log.e(LOGTAG, "Permiso denegado");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PETICION_CONFIG_UBICACION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(LOGTAG, "El usuario no ha realizado los cambios de configuración necesarios");
                        btnActualizar.setChecked(false);
                        break;
                }
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i(LOGTAG, "Recibida nueva ubicación!");

        //Mostramos la nueva ubicación recibida
        updateUI(location);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       Intent explicit_intent;

        switch (item.getItemId()){
            case R.id.action_exit:
                guardarpreferencias();
                //poner en marcha la publicidad al salir
                MainActivity.this.startAppAd.showAd();
                MainActivity.this.startAppAd.loadAd();
                finish();
                break;


        }

        return super.onOptionsItemSelected(item);
    }


}
