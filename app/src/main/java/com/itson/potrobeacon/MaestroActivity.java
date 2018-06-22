package com.itson.potrobeacon;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Response;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itson.potrobeacon.Datos.Alumno;
import com.itson.potrobeacon.Datos.Clase;
import com.itson.potrobeacon.Datos.Salon;
import com.itson.potrobeacon.Datos.Salones;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MaestroActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, BeaconConsumer, RangeNotifier {

    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS};
    private String spreadsheetId;

    private BeaconManager mBeaconManager;
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference referencia = database.getReference("message");

    private TextView txtConsola;
    private EditText txtIdBeacon;
    private EditText txtIdAlumno;

    private EditText txtIdClase;
    private EditText txtNombreClase;
    private Spinner spinnerEntrada;
    private Spinner spinnerSalida;

    private String idUsuario;
    private String nombre;

    private int idBeaconConectado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maestro);


        txtIdBeacon = (EditText) findViewById(R.id.txtIdBeacon);
        txtIdAlumno = (EditText) findViewById(R.id.txtIdAlumno);
        txtIdClase = (EditText) findViewById(R.id.txtIdClase);
        txtNombreClase = (EditText) findViewById(R.id.txtNombre);
        txtConsola = (TextView)findViewById(R.id.txtConsola);
        spinnerEntrada = (Spinner) findViewById(R.id.spinnerEntrada);
        spinnerSalida = (Spinner) findViewById(R.id.spinnerSalida);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(),
                R.array.horas, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEntrada.setAdapter(adapter);
        spinnerSalida.setAdapter(adapter);

        idUsuario = getSharedPreferences("PotroBeacon", 0).getString("id", "nada").toString();
        nombre = getSharedPreferences("PotroBeacon", 0).getString("nombre", "nada").toString();

        ///////////////////////////Beacons///////////////////////////////////////
        mBeaconManager = BeaconManager.getInstanceForApplication(this);
        // In this example, we will use Eddystone protocol, so we have to define it here
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // Binds this activity to the BeaconService
        mBeaconManager.bind(this);

        idBeaconConectado = -1;

        ////////////////////////////GOOGLE SHEETS////////////////////////////////
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Sheets API ...");

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }

    @Override
    public void onDestroy () {
        try
        {
            mBeaconManager.stopRangingBeaconsInRegion(region);
            ApagarProyector(idBeaconConectado);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public void AgregarBeacon(View v)
    {
        DatabaseReference referencia = database.getReference("Salones");
        Map<String,String> mapJunk = new HashMap<String,String>();
        mapJunk.put("junk", "junk");
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("clases", mapJunk);
        map.put("id", Integer.parseInt(txtIdBeacon.getText().toString()));
        referencia.child("Beacon " + txtIdBeacon.getText().toString()).setValue(map);
    }

    int numClases;
    public void AgregarClase(View v)
    {
        String entrada = spinnerEntrada.getSelectedItem().toString().substring(0,2) + spinnerEntrada.getSelectedItem().toString().substring(3,5);
        String salida = spinnerSalida.getSelectedItem().toString().substring(0,2) + spinnerSalida.getSelectedItem().toString().substring(3,5);

        final Clase clase = new Clase();
        clase.setId(Integer.parseInt(txtIdClase.getText().toString()));
        clase.setNombre(txtNombreClase.getText().toString());
        clase.setEntrada(Integer.parseInt(entrada));
        clase.setSalida(Integer.parseInt(salida));
        clase.setSpreadSheetId(ObtenerSheet());
        Map<String,String> alumno = new HashMap<String,String>();
        alumno.put("" + idUsuario, "" + idUsuario);
        alumno.put("junk", "junk");
        clase.setAlumnos(alumno);

        DatabaseReference referencia = database.getReference("Salones/Beacon " + txtIdBeacon.getText().toString() + "/clases");
        referencia.child(clase.getId() + "").setValue(clase);
    }

    int posicionClase;
    int numAlumnos;
    public void AgregarAlumno(View v)
    {
        DatabaseReference referencia = database.getReference("Salones/Beacon " + txtIdBeacon.getText().toString() + "/clases/" + txtIdClase.getText().toString() + "/alumnos");
        referencia.child(txtIdAlumno.getText().toString()).setValue(txtIdAlumno.getText().toString());

    }

    private Region region;
    @Override
    public void onBeaconServiceConnect() {
        // Encapsulates a beacon identifier of arbitrary byte length
        ArrayList<Identifier> identifiers = new ArrayList<>();

        // Set null to indicate that we want to match beacons with any value
        identifiers.add(null);
        // Represents a criteria of fields used to match beacon
        region = new Region("AllBeaconsRegion", identifiers);
        try {
            // Tells the BeaconService to start looking for beacons that match the passed Region object
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        // Specifies a class that should be called each time the BeaconService gets ranging data, once per second by default
        mBeaconManager.addRangeNotifier(this);
    }

    boolean ocupado;
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region)
    {
        if(!ocupado)
        {
            ocupado = true;
            if (beacons.size() > 0) {
                if (idBeaconConectado == -1) {
                    txtConsola.setText("Maestro \n");

                    int tamano = beacons.size();
                    Beacon[] listaBeacons = beacons.toArray(new Beacon[tamano]);
                    for (int i = 0; i < tamano; i++) {
                        txtConsola.setText(txtConsola.getText().toString() + "Distancia en metros:" + listaBeacons[i].getDistance() + "\n");
                        txtConsola.setText(txtConsola.getText().toString() + "Dirección Bluetooth:" + listaBeacons[i].getBluetoothAddress() + "\n");
                        txtConsola.setText(txtConsola.getText().toString() + "UUID:" + listaBeacons[i].getId1() + "\n");
                        txtConsola.setText(txtConsola.getText().toString() + "ID de fabricante:" + listaBeacons[i].getManufacturer() + "\n");
                        VerificarClase(listaBeacons[i].getManufacturer());
                    }
                }
            }
            else
            {
                if (idBeaconConectado != -1) {
                    ApagarProyector(idBeaconConectado);
                    idBeaconConectado = -1;
                }
            }
            ocupado = false;
        }
    }

    public void VerificarClase(final int id)
    {
        referencia = database.getReference("Salones/Beacon " + id + "");
        // Attach a listener to read the data at our posts reference
        referencia.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                try
                {

                    Salon salon = dataSnapshot.getValue(Salon.class);

                    txtConsola.setText(txtConsola.getText() + "id Salon:" + salon.getId() + "\n");
                    txtConsola.setText(txtConsola.getText() + "Número de clases:" + salon.getClases().size() + "\n");

                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> mapClases = salon.getClases();

                    Map<String, Object> map = mapClases;
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        try {
                            Clase clase = mapper.convertValue(entry.getValue(), Clase.class);

                            txtConsola.setText(txtConsola.getText() + "id de clase:" + clase.getId() + "\n");
                            txtConsola.setText(txtConsola.getText() + "Nombre de clase:" + clase.getNombre() + "\n");
                            txtConsola.setText(txtConsola.getText() + "entrada:" + clase.getEntrada() + "\n");
                            txtConsola.setText(txtConsola.getText() + "salida:" + clase.getSalida() + "\n");
                            txtConsola.setText(txtConsola.getText() + "SpreadSheetId:" + clase.getSpreadSheetId() + "\n");

                            Date tiempo = Calendar.getInstance().getTime();

                            //Hacemos esto para cuando la hora o los minutos son de un solo digito
                            String hora = tiempo.getHours() + "", minutos = tiempo.getMinutes() + "";
                            if (String.valueOf(hora).length() < 2)
                                hora = "0" + hora;
                            if (String.valueOf(minutos).length() < 2)
                                minutos = "0" + minutos;

                            int horaFinal = Integer.parseInt(hora + "" + minutos);
                            System.out.println("Hora actual:" + horaFinal);

                            if (horaFinal > clase.getEntrada() && horaFinal < clase.getSalida()) {
                                System.out.println("A tiempo, buscando en la lista de alumnos");
                                Map<String, String> mapAlumnos = clase.getAlumnos();
                                for (Map.Entry<String, String> entrada : mapAlumnos.entrySet()) {
                                    Alumno alumno = mapper.convertValue(entrada.getValue(), Alumno.class);

                                    txtConsola.setText(txtConsola.getText() + "id:" + alumno.getId() + "\n");
                                    if (alumno.getId().toString().equalsIgnoreCase(idUsuario)) {
                                        System.out.println("Sí está en la lista");
                                        idBeaconConectado = id;
                                        AbrirPuerta(idBeaconConectado);
                                        EncenderProyector(idBeaconConectado);
                                        return;
                                    }
                                }
                                System.out.println("No está en la lista, pasando a otra clase");
                            } else {
                                System.out.println("Tarde o muy temprano, terminando");
                                return;
                            }

                            txtConsola.setText(txtConsola.getText() + "\n");
                        } catch (Exception ex) {
                            System.out.println(ex);
                        }
                    }
                }
                catch(Exception ex)
                {
                    System.out.println(ex);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    public String ObtenerSheet()
    {
        getResultsFromApi();
        return spreadsheetId;
    }

    public void EncenderProyector(int id)
    {
        new HttpRequestAsync("https://potro.herokuapp.com/api/lv" + id + "/projector/on").execute();
    }

    public void ApagarProyector(int id)
    {
        new HttpRequestAsync("https://potro.herokuapp.com/api/lv" + id + "/projector/off").execute();
    }

    public void AbrirPuerta(int id)
    {
        new HttpRequestAsync("https://potro.herokuapp.com/api/lv" + id + "/door").execute();
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable())
        {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            //mOutputText.setText("No network connection available.");
        } else
        {
            new MaestroActivity.MakeRequestTask(mCredential).execute();
        }
    }


    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, android.Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    android.Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK)
                {
                    //mOutputText.setText("This app requires Google Play Services. Please install " + "Google Play Services on your device and relaunch this app.");
                } else
                {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MaestroActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>>
    {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential)
        {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try
            {
                mProgress.dismiss();
                return CrearSheet();
            }
            catch (Exception e)
            {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         * @return List of names and majors
         * @throws IOException
         */
        private List<String> CrearSheet() throws IOException
        {
            Spreadsheet ss = new Spreadsheet();
            SpreadsheetProperties ssp = new SpreadsheetProperties();
            ssp.setTitle(txtNombreClase.getText().toString() + " " + spinnerEntrada.getSelectedItem().toString());
            ss.setProperties(ssp);

            Sheets.Spreadsheets.Create create = mService.spreadsheets().create(ss);

            create.setOauthToken(SheetsScopes.SPREADSHEETS);

            ss = create.execute();

            spreadsheetId = ss.getSpreadsheetId();

            DatabaseReference referencia = database.getReference("Salones/Beacon " + txtIdBeacon.getText().toString() + "/clases/" + txtIdClase.getText().toString());

            referencia.child("spreadSheetId").setValue(ss.getSpreadsheetId());

            EscribirSheet("a1", "Nombre/Fecha");
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            //int dia = cal.get(Calendar.DAY_OF_MONTH) + 1;
            for (int i = 1; i < 32; i++)
                EscribirSheet("a" + (i + 1), i + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR));

            return null;
        }

        private void EscribirSheet(String coordenada, String contenido) throws IOException
        {
            System.out.println("Escribiendo en coordenadas:" + coordenada + ", el contenido:" + contenido);
            ValueRange values = new ValueRange();
            values.setValues(
                    Arrays.asList(
                            Arrays.asList((Object)contenido)));

            this.mService.spreadsheets().values()
                    .update(spreadsheetId, coordenada, values)
                    .setValueInputOption("RAW")
                    .execute();
        }

        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                //mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Sheets API:");
                //mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    //mOutputText.setText("The following error occurred:\n" + mLastError.getMessage());
                }
            } else {
                //mOutputText.setText("Request cancelled.");
            }
        }
    }

    /*
    private class HttpRequestAsync extends AsyncTask<Void, Void, Void>
    {
        private String url;

        public HttpRequestAsync(String url)
        {
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                URL url = new URL(this.url);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try
                {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    System.out.println(this.url);
                    System.out.println(txtConsola.getText().toString() + " " + readStream(in));
                }
                finally
                {
                    urlConnection.disconnect();
                }
            }
            catch(Exception ex)
            {

            }
            return null;
        }

        protected void onPostExecute(Boolean result)
        {

        }

        private String readStream(InputStream is) {
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                int i = is.read();
                while(i != -1) {
                    bo.write(i);
                    i = is.read();
                }
                return bo.toString();
            } catch (IOException e) {
                return "";
            }
        }
    }
    */
}
