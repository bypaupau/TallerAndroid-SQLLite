package com.example.myapplication;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private EditText txtid;
    private EditText txtnombre;
    private EditText txtapellido;
    FeedReaderDbHelper dbHelper = new FeedReaderDbHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // ESTA LINEA ES LA QUE YA VIENE POR DEFAULT

        txtid = findViewById(R.id.txtid);
        txtnombre = findViewById(R.id.txtnombre);
        txtapellido = findViewById(R.id.txtapellido);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // metodos que van a los botones
    public void Listar(View vista){
        Intent listar = new Intent(this,Listado.class);
        startActivity(listar);
    }

    public void guardar(View vista){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.column1, txtnombre.getText().toString());
        values.put(FeedReaderContract.FeedEntry.column2, txtapellido.getText().toString());
        long newRodId = db.insert(FeedReaderContract.FeedEntry.nameTable, null, values);
        Toast.makeText(getApplicationContext(), "se guardó el registro con la clave" +
                newRodId,Toast.LENGTH_LONG).show();
        db.close();
    }

    public void Buscar(View vista){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                BaseColumns._ID,
                FeedReaderContract.FeedEntry.column1,
                FeedReaderContract.FeedEntry.column2
        };

        String selection = FeedReaderContract.FeedEntry._ID + " = ?";
        String[] selectionArgs = {txtid.getText().toString()};
        String sortOrder = FeedReaderContract.FeedEntry.column2 + " ASC";
        Cursor cursor = db.query(
                FeedReaderContract.FeedEntry.nameTable, // la tabla a querear
                projection,  // arrar de columns a retornar
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );

        while(cursor.moveToNext()){
            String nombre=cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.column1));
            txtnombre.setText(nombre+"");
            String apellido = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.column2));
            txtapellido.setText(apellido+"");
        }
        cursor.close();
        db.close();
    }

    public void Eliminar(View vista){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = FeedReaderContract.FeedEntry._ID + " = ?";
        String[] selectionArgs = { txtid.getText().toString()};
        int deletedRows = db.delete(FeedReaderContract.FeedEntry.nameTable, selection, selectionArgs);
        db.close();
        Toast.makeText(getApplicationContext(), "se eliminó " +
                deletedRows + " registro(s)", Toast.LENGTH_LONG).show();}


    public void Actualizar(View vista){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String nombre = txtnombre.getText().toString();
        String apellido = txtapellido.getText().toString();
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.column1, nombre);
        values.put(FeedReaderContract.FeedEntry.column2, apellido);
        String selection = FeedReaderContract.FeedEntry._ID + " = ?";
        String[] selectionArgs = {txtid.getText().toString()};
        int count = db.update(FeedReaderContract.FeedEntry.nameTable,
                values,
                selection,
                selectionArgs);
        Toast.makeText(getApplicationContext(), "se actualizó" +
                count + " registros(s)", Toast.LENGTH_LONG).show();
        db.close();
    }
}

