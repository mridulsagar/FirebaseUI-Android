/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.firebase.ui.auth.data.model.User;
import com.firebase.ui.auth.util.ExtraConstants;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;

/**
 * A container that encapsulates the result of authenticating with an Identity Provider.
 */
public class IdpResponse implements Parcelable {
    public static final Creator<IdpResponse> CREATOR = new Creator<IdpResponse>() {
        @Override
        public IdpResponse createFromParcel(Parcel in) {
            return new IdpResponse(
                    in.<User>readParcelable(User.class.getClassLoader()),
                    in.readString(),
                    in.readString(),
                    (AuthCredential) in.readParcelable(AuthCredential.class.getClassLoader()),
                    (FirebaseUiException) in.readSerializable()
            );
        }

        @Override
        public IdpResponse[] newArray(int size) {
            return new IdpResponse[size];
        }
    };

    private final User mUser;

    private final String mToken;
    private final String mSecret;

    private final FirebaseUiException mException;

    private final AuthCredential mPendingCredential;

    private IdpResponse(@NonNull FirebaseUiException e) {
        this(null, null, null, null, e);
    }

    private IdpResponse(@NonNull AuthCredential pendingCredential) {
        this(
                null, null, null, /* user, token, and secret */
                pendingCredential,
                new FirebaseUiException(ErrorCodes.ANONYMOUS_UPGRADE_MERGE_CONFLICT));
    }

    private IdpResponse(
            @NonNull User user,
            @Nullable String token,
            @Nullable String secret) {
        this(user, token, secret, null, null);
    }

    private IdpResponse(
            @Nullable User user,
            @Nullable String token,
            @Nullable String secret,
            @Nullable AuthCredential pendingCredential,
            @Nullable FirebaseUiException e) {
        mUser = user;
        mToken = token;
        mSecret = secret;
        mPendingCredential = pendingCredential;
        mException = e;
    }

    /**
     * Extract the {@link IdpResponse} from the flow's result intent.
     *
     * @param resultIntent The intent which {@code onActivityResult} was called with.
     * @return The IdpResponse containing the token(s) from signing in with the Idp
     */
    @Nullable
    public static IdpResponse fromResultIntent(@Nullable Intent resultIntent) {
        if (resultIntent != null) {
            return resultIntent.getParcelableExtra(ExtraConstants.EXTRA_IDP_RESPONSE);
        } else {
            return null;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Intent getErrorIntent(@NonNull Exception e) {
        return fromError(e).toIntent();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static IdpResponse fromError(@NonNull Exception e) {
        if (e instanceof FirebaseUiException) {
            return new IdpResponse((FirebaseUiException) e);
        } else {
            return new IdpResponse(new FirebaseUiException(ErrorCodes.UNKNOWN_ERROR, e));
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Intent toIntent() {
        return new Intent().putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, this);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isSuccessful() {
        return mException == null;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public User getUser() {
        return mUser;
    }

    /**
     * Get the type of provider. e.g. {@link GoogleAuthProvider#PROVIDER_ID}
     */
    @NonNull
    @AuthUI.SupportedProvider
    public String getProviderType() {
        return mUser.getProviderId();
    }

    /**
     * Get the email used to sign in.
     */
    @Nullable
    public String getEmail() {
        return mUser.getEmail();
    }

    /**
     * Get the phone number used to sign in.
     */
    @Nullable
    public String getPhoneNumber() {
        return mUser.getPhoneNumber();
    }

    /**
     * Get the token received as a result of logging in with the specified IDP
     */
    @Nullable
    public String getIdpToken() {
        return mToken;
    }

    /**
     * Twitter only. Return the token secret received as a result of logging in with Twitter.
     */
    @Nullable
    public String getIdpSecret() {
        return mSecret;
    }

    /**
     * TODO: Docs and return type
     */
    @Nullable
    public AuthCredential getPendingCredential() {
        return mPendingCredential;
    }

    /**
     * Get the error code for a failed sign in
     *
     * @deprecated use {@link #getError()} instead
     */
    @Deprecated
    public int getErrorCode() {
        if (isSuccessful()) {
            return Activity.RESULT_OK;
        } else {
            return mException.getErrorCode();
        }
    }

    /**
     * Get the error for a failed sign in.
     */
    @Nullable
    public FirebaseUiException getError() {
        return mException;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mUser, flags);
        dest.writeString(mToken);
        dest.writeString(mSecret);
        dest.writeParcelable(mPendingCredential, 0);
        dest.writeSerializable(mException);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdpResponse response = (IdpResponse) o;

        return (mUser == null ? response.mUser == null : mUser.equals(response.mUser))
                && (mToken == null ? response.mToken == null : mToken.equals(response.mToken))
                && (mSecret == null ? response.mSecret == null : mSecret.equals(response.mSecret))
                && (mException == null ? response.mException == null : mException.equals(response.mException));
    }

    @Override
    public int hashCode() {
        int result = mUser == null ? 0 : mUser.hashCode();
        result = 31 * result + (mToken == null ? 0 : mToken.hashCode());
        result = 31 * result + (mSecret == null ? 0 : mSecret.hashCode());
        result = 31 * result + (mException == null ? 0 : mException.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "IdpResponse{" +
                "mUser=" + mUser +
                ", mToken='" + mToken + '\'' +
                ", mSecret='" + mSecret + '\'' +
                ", mException=" + mException +
                '}';
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Builder {

        private final User mUser;
        private final AuthCredential mCredential;

        private String mToken;
        private String mSecret;

        public Builder(@NonNull User user) {
            mUser = user;
            mCredential = null;
        }

        public Builder(@NonNull AuthCredential pendingCredential) {
            mUser = null;
            mCredential = pendingCredential;
        }

        public Builder setToken(String token) {
            mToken = token;
            return this;
        }

        public Builder setSecret(String secret) {
            mSecret = secret;
            return this;
        }

        public IdpResponse build() {
            if (mUser != null) {
                String providerId = mUser.getProviderId();
                if (!AuthUI.SUPPORTED_PROVIDERS.contains(providerId)) {
                    throw new IllegalStateException("Unknown provider: " + providerId);
                }
                if (AuthUI.SOCIAL_PROVIDERS.contains(providerId) && TextUtils.isEmpty(mToken)) {
                    throw new IllegalStateException(
                            "Token cannot be null when using a non-email provider.");
                }
                if (providerId.equals(TwitterAuthProvider.PROVIDER_ID)
                        && TextUtils.isEmpty(mSecret)) {
                    throw new IllegalStateException(
                            "Secret cannot be null when using the Twitter provider.");
                }

                return new IdpResponse(mUser, mToken, mSecret);
            } else if (mCredential != null) {
                return new IdpResponse(mCredential);
            } else {
                throw new IllegalStateException(
                        "IdpResponse requires either a User or an AuthCredential.");
            }
        }
    }
}
