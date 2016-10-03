/*
 * Copyright (c) 2015 Spotify AB
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.sdk.android.authentication;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * An object that helps construct the request that is sent to Spotify authentication service.
 * To create one use {@link com.spotify.sdk.android.authentication.AuthenticationRequest.Builder}
 *
 * @see <a href="https://developer.spotify.com/web-api/authorization-guide">Web API Authorization guide</a>
 */
public class AuthenticationRequest implements Parcelable {

    static final String ACCOUNTS_SCHEME = "https";
    static final String ACCOUNTS_AUTHORITY = "accounts.spotify.com";
    static final String ACCOUNTS_PATH = "authorize";
    static final String SCOPES_SEPARATOR = " ";

    static final class QueryParams {
        public static final String CLIENT_ID = "client_id";
        public static final String RESPONSE_TYPE = "response_type";
        public static final String REDIRECT_URI = "redirect_uri";
        public static final String STATE = "state";
        public static final String SCOPE = "scope";
        public static final String SHOW_DIALOG = "show_dialog";
    }

    private final String mClientId;
    private final String mResponseType;
    private final String mRedirectUri;
    private final String mState;
    private final String[] mScopes;
    private final Map<String, String> mCustomParams;

    /**
     * Use this builder to create an {@link com.spotify.sdk.android.authentication.AuthenticationRequest}
     *
     * @see com.spotify.sdk.android.authentication.AuthenticationRequest
     */
    public static class Builder {

        private final String mClientId;
        private final AuthenticationResponse.Type mResponseType;
        private final String mRedirectUri;

        private String mState;
        private String[] mScopes;
        private final Map<String, String> mCustomParams = new HashMap<>();

        public Builder(String clientId, AuthenticationResponse.Type responseType, String redirectUri) {
            if (clientId == null) {
                throw new IllegalArgumentException("Client ID can't be null");
            }
            if (responseType == null) {
                throw new IllegalArgumentException("Response type can't be null");
            }
            if (redirectUri == null || redirectUri.length() == 0) {
                throw new IllegalArgumentException("Redirect URI can't be null or empty");
            }

            mClientId = clientId;
            mResponseType = responseType;
            mRedirectUri = redirectUri;
        }

        public Builder setState(String state) {
            mState = state;
            return this;
        }

        public Builder setScopes(String[] scopes) {
            mScopes = scopes;
            return this;
        }

        public Builder setCustomParam(String key, String value) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Custom parameter key can't be null or empty");
            }
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("Custom parameter value can't be null or empty");
            }
            mCustomParams.put(key, value);
            return this;
        }

        public AuthenticationRequest build() {
            return new AuthenticationRequest(mClientId, mResponseType, mRedirectUri,
                    mState, mScopes, mCustomParams);
        }
    }

    public AuthenticationRequest(Parcel source) {
        mClientId = source.readString();
        mResponseType = source.readString();
        mRedirectUri = source.readString();
        mState = source.readString();
        mScopes = source.createStringArray();

        mCustomParams = new HashMap<>();
        Bundle bundle = source.readBundle(getClass().getClassLoader());
        for (String key : bundle.keySet()) {
            mCustomParams.put(key, bundle.getString(key));
        }
    }

    public String getClientId() {
        return mClientId;
    }

    public String getResponseType() {
        return mResponseType;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }

    public String getState() {
        return mState;
    }

    public String[] getScopes() {
        return mScopes;
    }

    public String getCustomParam(String key) {
        return mCustomParams.get(key);
    }

    private AuthenticationRequest(String clientId,
                                  AuthenticationResponse.Type responseType,
                                  String redirectUri,
                                  String state,
                                  String[] scopes,
                                  Map<String, String> customParams) {

        mClientId = clientId;
        mResponseType = responseType.toString();
        mRedirectUri = redirectUri;
        mState = state;
        mScopes = scopes;
        mCustomParams = customParams;
    }

    public Uri toUri() {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(ACCOUNTS_SCHEME)
                .authority(ACCOUNTS_AUTHORITY)
                .appendPath(ACCOUNTS_PATH)
                .appendQueryParameter(QueryParams.CLIENT_ID, mClientId)
                .appendQueryParameter(QueryParams.RESPONSE_TYPE, mResponseType)
                .appendQueryParameter(QueryParams.REDIRECT_URI, mRedirectUri)
                .appendQueryParameter(QueryParams.SHOW_DIALOG, "true");

        if (mScopes != null && mScopes.length > 0) {
            uriBuilder.appendQueryParameter(QueryParams.SCOPE, scopesToString());
        }

        if (mState != null) {
            uriBuilder.appendQueryParameter(QueryParams.STATE, mState);
        }

        if (mCustomParams.size() > 0) {
            for (Map.Entry<String, String> entry : mCustomParams.entrySet()) {
                uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        return uriBuilder.build();
    }

    private String scopesToString() {
        StringBuilder concatScopes = new StringBuilder();
        for (String scope : mScopes) {
            concatScopes.append(scope);
            concatScopes.append(SCOPES_SEPARATOR);
        }
        return concatScopes.toString().trim();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mClientId);
        dest.writeString(mResponseType);
        dest.writeString(mRedirectUri);
        dest.writeString(mState);
        dest.writeStringArray(mScopes);

        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : mCustomParams.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<AuthenticationRequest> CREATOR = new Parcelable.Creator<AuthenticationRequest>() {

        @Override
        public AuthenticationRequest createFromParcel(Parcel source) {
            return new AuthenticationRequest(source);
        }

        @Override
        public AuthenticationRequest[] newArray(int size) {
            return new AuthenticationRequest[size];
        }
    };
}
