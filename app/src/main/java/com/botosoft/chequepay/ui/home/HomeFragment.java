package com.botosoft.chequepay.ui.home;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.botosoft.chequepay.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {


    private HomeViewModel homeViewModel;
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton, next, upload;
    private ImageView frame, icon1, icon2, icon3, icon4, doneImage;
    private TextView text1, text2, doneText, header;
    Spinner originAccount, destinationAccount;
    EditText name, BVN;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private Uri filePath, file_path;

    //Firebase
    FirebaseStorage storage;
    StorageReference storageReference;

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String UserIdKey = "UserIdKey";

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    final DatabaseReference usersID = database.getReference().child("userId");
    final DatabaseReference cheques = database.getReference().child("cheques");

    SharedPreferences sharedpreferences;

    String _userId;


    private final int PICK_IMAGE_REQUEST = 71;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        /*final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });*/
        initUi(root);
        return root;
    }

    public void initUi(View view){

        sharedpreferences = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        _userId = sharedpreferences.getString("UserIdKey", null);


        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        doneImage = (ImageView) view.findViewById(R.id.doneImage);
        doneText = (TextView) view.findViewById(R.id.doneText);
        header = (TextView) view.findViewById(R.id.header);

        originAccount = (Spinner) view.findViewById(R.id.origin_account);
        destinationAccount = (Spinner) view.findViewById(R.id.destination_account);

        doneImage.setVisibility(View.INVISIBLE);
        doneText.setVisibility(View.INVISIBLE);
        header.setVisibility(View.INVISIBLE);


        destinationAccount = (Spinner) view.findViewById(R.id.destination_account);
        name = (EditText) view.findViewById(R.id.name);
        BVN = (EditText) view.findViewById(R.id.BVN);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.age));
        dataAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        originAccount.setAdapter(dataAdapter);
        destinationAccount.setAdapter(dataAdapter);


        text1 = (TextView) view.findViewById(R.id.text1);
        text2 = (TextView) view.findViewById(R.id.text2);
        icon1 = (ImageView) view.findViewById(R.id.icon1);
        icon2 = (ImageView) view.findViewById(R.id.icon2);
        icon3 = (ImageView) view.findViewById(R.id.icon3);
        icon4 = (ImageView) view.findViewById(R.id.icon4);


        textureView = (TextureView) view.findViewById(R.id.texture);
        frame = (ImageView) view.findViewById(R.id.frame);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        textureView.setVisibility(View.GONE);
        frame.setVisibility(View.GONE);

        takePictureButton = (Button) view.findViewById(R.id.btn_takepicture);
        next = (Button) view.findViewById(R.id.btn_next);
        upload = (Button) view.findViewById(R.id.btn_upload);

        takePictureButton.setVisibility(View.INVISIBLE);
        upload.setVisibility(View.INVISIBLE);

        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();



            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(validity()){
                    header.setVisibility(View.VISIBLE);
                    text1.setVisibility(View.INVISIBLE);
                    text2.setVisibility(View.INVISIBLE);
                    icon1.setVisibility(View.INVISIBLE);
                    icon2.setVisibility(View.INVISIBLE);
                    icon3.setVisibility(View.INVISIBLE);
                    icon4.setVisibility(View.INVISIBLE);
                    textureView.setVisibility(View.VISIBLE);
                    frame.setVisibility(View.VISIBLE);
                    next.setVisibility(View.INVISIBLE);
                    originAccount.setVisibility(View.INVISIBLE);
                    destinationAccount.setVisibility(View.INVISIBLE);
                    name.setVisibility(View.INVISIBLE);
                    BVN.setVisibility(View.INVISIBLE);
                    takePictureButton.setVisibility(View.VISIBLE);
                    upload.setVisibility(View.VISIBLE);
                }


            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });


    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here

            frame.setVisibility(View.VISIBLE);
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            filePath = data.getData();
            file_path = filePath;


            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), filePath);
                uploadImage();
                //imageView.setImageBitmap(bitmap);
            }

            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
            frame.setVisibility(View.INVISIBLE);
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
            frame.setVisibility(View.INVISIBLE);
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //Toast.makeText(getActivity(), "Saved:" + file, Toast.LENGTH_SHORT).show();
            uploadImage();
            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 380;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory() + "/"+ UUID.randomUUID().toString() + "pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        file_path = Uri.fromFile(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Toast.makeText(getActivity(), "Saved:" + file, Toast.LENGTH_SHORT).show();
                    uploadImage();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        uploadImage();
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getActivity(), "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(getActivity(), "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                //finish();
            }
        }
    }


    private void uploadImage() {

        if(file_path != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            StorageReference ref = storageReference.child("cheques/"+ UUID.randomUUID().toString());
            ref.putFile(file_path)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            Date date = new Date();
                            //Timestamp timestamp =new Timestamp(date.getTime());

                            Date currentDate = new Date();
                            long timestamp = currentDate.getTime() / 1000;


                            String _date = formatter.format(date);

                            Log.e("error", String.valueOf(date.getSeconds()));
                            //save for user
                            usersID.child(_userId).child("cheques").child(String.valueOf(timestamp)).child("originAccount").setValue(originAccount.getSelectedItem().toString());
                            usersID.child(_userId).child("cheques").child(String.valueOf(timestamp)).child("destinationAccount").setValue(destinationAccount.getSelectedItem().toString());
                            usersID.child(_userId).child("cheques").child(String.valueOf(timestamp)).child("name").setValue(name.getText().toString());
                            usersID.child(_userId).child("cheques").child(String.valueOf(timestamp)).child("bvn").setValue(BVN.getText().toString());
                            usersID.child(_userId).child("cheques").child(String.valueOf(timestamp)).child("timestamp").setValue(timestamp);
                            usersID.child(_userId).child("cheques").child(String.valueOf(timestamp)).child("date").setValue(_date);

                            //save to cheques db
                            cheques.child(String.valueOf(timestamp)).child("cheques").child("originAccount").setValue(originAccount.getSelectedItem().toString());
                            cheques.child(String.valueOf(timestamp)).child("cheques").child("user_id").setValue(_userId);
                            cheques.child(String.valueOf(timestamp)).child("cheques").child("destinationAccount").setValue(destinationAccount.getSelectedItem().toString());
                            cheques.child(String.valueOf(timestamp)).child("cheques").child("name").setValue(name.getText().toString());
                            cheques.child(String.valueOf(timestamp)).child("cheques").child("bvn").setValue(BVN.getText().toString());
                            cheques.child(String.valueOf(timestamp)).child("cheques").child("timestamp").setValue(String.valueOf(timestamp));
                            cheques.child(String.valueOf(timestamp)).child("cheques").child("date").setValue(_date);

                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "Uploaded", Toast.LENGTH_SHORT).show();

                            doneImage.setVisibility(View.VISIBLE);
                            doneText.setVisibility(View.VISIBLE);


                            header.setVisibility(View.INVISIBLE);
                            text1.setVisibility(View.INVISIBLE);
                            text2.setVisibility(View.INVISIBLE);
                            icon1.setVisibility(View.INVISIBLE);
                            icon2.setVisibility(View.INVISIBLE);
                            icon3.setVisibility(View.INVISIBLE);
                            icon4.setVisibility(View.INVISIBLE);
                            textureView.setVisibility(View.INVISIBLE);
                            frame.setVisibility(View.INVISIBLE);
                            next.setVisibility(View.INVISIBLE);
                            originAccount.setVisibility(View.INVISIBLE);
                            destinationAccount.setVisibility(View.INVISIBLE);
                            name.setVisibility(View.INVISIBLE);
                            BVN.setVisibility(View.INVISIBLE);
                            takePictureButton.setVisibility(View.INVISIBLE);
                            upload.setVisibility(View.INVISIBLE);



                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploading... ");
                            //progressDialog.setMessage("Uploading "+(int)progress+"%");
                        }
                    });
        }
    }


    public boolean validity(){
        boolean valid = true;
        if(originAccount.getSelectedItem().toString().equals("-Select a bank-")){
            Toast.makeText(getActivity(), "Please select a origin bank", Toast.LENGTH_SHORT).show();
            valid = false;
        }else if(destinationAccount.getSelectedItem().toString().equals("-Select a bank-")){
            Toast.makeText(getActivity(), "Please select a destination bank", Toast.LENGTH_SHORT).show();
            valid = false;
        }else if(name.getText().length() == 0){
            Toast.makeText(getActivity(), "Please enter your name", Toast.LENGTH_SHORT).show();
            valid = false;
        }else if(BVN.getText().length() != 11){
            Toast.makeText(getActivity(), "Invalid BVN number, it should be 11 digits", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }



}