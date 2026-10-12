package com.android.sistemabodegaappandroid.session;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.android.sistemabodegaappandroid.LoginActivity;
import com.android.sistemabodegaappandroid.model.Usuario;

public class SessionManager {
    private static final String PREF_NAME = "SistemaBodegaSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NOMBRE_COMPLETO = "nombreCompleto";
    private static final String KEY_ROLE_ID = "roleId";
    private static final String KEY_ROLE_NAME = "roleName";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(Usuario usuario) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, usuario.getIdUsuario());
        editor.putString(KEY_USERNAME, usuario.getUsername());
        editor.putString(KEY_NOMBRE_COMPLETO, usuario.getNombreCompleto());
        editor.putInt(KEY_ROLE_ID, usuario.getIdRol());
        editor.putString(KEY_ROLE_NAME, usuario.getNombreRol() != null ? usuario.getNombreRol() : "");
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, "");
    }

    public String getNombreCompleto() {
        return pref.getString(KEY_NOMBRE_COMPLETO, "");
    }

    public int getRoleId() {
        return pref.getInt(KEY_ROLE_ID, -1);
    }

    public String getRoleName() {
        return pref.getString(KEY_ROLE_NAME, "");
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
