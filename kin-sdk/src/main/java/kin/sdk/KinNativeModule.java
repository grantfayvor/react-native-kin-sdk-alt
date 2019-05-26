package kin.sdk;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.IllegalViewOperationException;

import java.math.BigDecimal;

import kin.base.xdr.Error;
import kin.base.xdr.ErrorCode;
import kin.sdk.exception.CreateAccountException;
import kin.utils.Request;
import kin.utils.ResultCallback;

public class KinNativeModule extends ReactContextBaseJavaModule {

    private KinClient kinClient;
    private KinAccount kinAccount;
    private String publicAddress;

    public KinNativeModule(ReactApplicationContext reactContext) {
        super(reactContext); //required by React Native
    }

    @Override
    //getName is required to define the name of the module represented in JavaScript
    public String getName() {
        return "KinNative";
    }

    @ReactMethod
    public void sayHi(Callback errorCallback, Callback successCallback) {
        try {
            System.out.println("Greetings from Java");
            successCallback.invoke("Callback : Greetings from Java");
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public KinClient getClient(String appId, String environment) {
        Environment environ;
        switch (environment) {
            case "production":
            case "PRODUCTION":
                environ = Environment.PRODUCTION;
                break;
            default:
                environ = Environment.TEST;
                break;
        }
        return kinClient = new KinClient(getReactApplicationContext(), environ, appId);
    }

    @ReactMethod
    public KinAccount createAccount() {
        try {
            if (!kinClient.hasAccount()) {
                kinAccount = kinClient.addAccount();
            }
        } catch (CreateAccountException ex) {
            ex.printStackTrace();
        }
        return kinAccount;
    }

    @ReactMethod
    public KinAccount getAccount() throws Exception {
        if (kinAccount != null) return kinAccount;
        if (kinClient.hasAccount()) return kinAccount = kinClient.getAccount(0);
        else throw new Exception("Cannot find an associated kin account for the kin client");
    }

    @ReactMethod
    public void deleteAccount(int index) throws Exception {
        if (!kinClient.hasAccount()) throw new Exception("Kin client does not have any account");
        kinClient.deleteAccount(index);
    }

    @ReactMethod
    public void deleteAccount() throws Exception {
        deleteAccount(0);
    }

    @ReactMethod
    public String getPublicAddress() {
        if (publicAddress != null) return publicAddress;
        return kinAccount.getPublicAddress();
    }

    @ReactMethod
    public void getStatus(final Callback cb) {
        Request<Integer> statusRequest = kinAccount.getStatus();
        statusRequest.run(new ResultCallback<Integer>() {
            @Override
            public void onResult(Integer result) {
                switch (result) {
                    case AccountStatus.CREATED:
                        cb.invoke(null, new Status("Kin account has been created", 200));
                        break;
                    case AccountStatus.NOT_CREATED:
                        Error err = new Error();
                        err.setCode(ErrorCode.ERR_DATA);
                        err.setMsg("Kin account has not been created");
                        cb.invoke(err);
                        break;
                }
            }

            @Override
            public void onError(Exception e) {
                cb.invoke(e);
            }
        });
    }

    @ReactMethod
    public void getBalance(final Callback cb) {
        Request<Balance> balanceRequest = kinAccount.getBalance();
        balanceRequest.run(new ResultCallback<Balance>() {

            @Override
            public void onResult(Balance result) {
                cb.invoke(null, result);
            }

            @Override
            public void onError(Exception e) {
                cb.invoke(e);
            }
        });
    }

    @ReactMethod
    public void buildTransaction (String recipientAddress, BigDecimal amount,  final Callback cb) {
        try {
            buildTransaction(recipientAddress, amount, (int) Math.ceil(getCurrentMinimumFee()), cb);
        } catch(Exception e) {
            cb.invoke(e);
        }
    }

    @ReactMethod
    public void buildTransaction(String recipientAddress, BigDecimal amount, int fee, final Callback cb) {
        Request<Transaction> transactionRequest = kinAccount.buildTransaction(recipientAddress, amount, fee);
        transactionRequest.run(new ResultCallback<Transaction>() {
            @Override
            public void onResult(Transaction transaction) {
                Request<TransactionId> sendTransactionRequest = kinAccount.sendTransaction(transaction);
                sendTransactionRequest.run(new ResultCallback<TransactionId>() {
                    @Override
                    public void onResult(TransactionId transactionId) {
                        cb.invoke(null, transactionId);
                    }

                    @Override
                    public void onError(Exception e) {
                        cb.invoke(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                cb.invoke(e);
            }
        });
    }

    @ReactMethod
    public long getCurrentMinimumFee() throws Exception{
        return kinClient.getMinimumFeeSync();
    }
}