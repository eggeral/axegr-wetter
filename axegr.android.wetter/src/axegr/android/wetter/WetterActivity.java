package axegr.android.wetter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BufferedHeader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class WetterActivity extends Activity {
	public static String TAG = "zamg";

	private List<File> imageBuffers_ = new ArrayList<File>();

	private int currentIndex_;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ImageView image = (ImageView) findViewById(R.id.imageView);
		image.setImageResource(R.drawable.test);

		new GetImage().execute((Void) null);
	}

	private class GetImage extends AsyncTask<Void, Void, List<File>> {

		@Override
		protected List<File> doInBackground(Void... params) {
			AndroidHttpClient client = AndroidHttpClient.newInstance("myuser",
					WetterActivity.this);
			List<File> buffers = new ArrayList<File>();
			try {
				String pageUri = "http://www.zamg.ac.at/wetter/wetteranimation/";
				HttpGet pageGet = new HttpGet(pageUri);
				HttpResponse pageResponse = client.execute(pageGet);

				ByteArrayOutputStream pageBuffer = new ByteArrayOutputStream();
				pageResponse.getEntity().writeTo(pageBuffer);
				String page = new String(pageBuffer.toByteArray());

				Pattern htmltag = Pattern
						.compile("imgnames\\[.*\\]\\s=\\s\"(.*)\";");
				List<String> imageNames = new ArrayList<String>();

				Matcher tagmatch = htmltag.matcher(page);
				while (tagmatch.find()) {
					imageNames.add(tagmatch.group(1));
				}

				for (String imageName : imageNames) {
					String baseUri = "http://www.zamg.ac.at/" + imageName;
					HttpGet get = new HttpGet(baseUri);
					HttpResponse response = client.execute(get);

					final ByteArrayOutputStream baos = new ByteArrayOutputStream();
					response.getEntity().writeTo(baos);

					byte[] bytes = baos.toByteArray();

					Matrix m = new Matrix();
					m.postRotate(90);

					Bitmap bmpOriginal = BitmapFactory.decodeByteArray(bytes,
							0, bytes.length);
					Bitmap bmpResult = Bitmap.createBitmap(bmpOriginal, 0, 0,
							bmpOriginal.getWidth(), bmpOriginal.getHeight(), m,
							true);

					File file = new File(getApplicationContext()
							.getExternalCacheDir(), "wetter/" + imageName);
					file.getParentFile().mkdirs();
									
					
					FileOutputStream fos = new FileOutputStream(file);
					bmpResult.compress(CompressFormat.PNG, 100, fos);

					// TODO: make this save
					fos.close();
					buffers.add(file);
				}

			} catch (IOException e) {
				Log.e(TAG, "error getting image", e);
			} finally {
				client.close();
			}
			return buffers;
		}

		@Override
		protected void onPostExecute(List<File> result) {
			imageBuffers_ = result;
			currentIndex_ = 0;
			ImageView image = (ImageView) findViewById(R.id.imageView);

			image.post(new UpdateImage());
		}

	}

	private class UpdateImage implements Runnable {
		@Override
		public void run() {
			ImageView image = (ImageView) findViewById(R.id.imageView);
			if (imageBuffers_.size() != 0) {

				if (currentIndex_ >= imageBuffers_.size()) {
					currentIndex_ = 0;
				}

				Bitmap bitmap = BitmapFactory.decodeFile(imageBuffers_.get(
						currentIndex_).getAbsolutePath());
				image.setImageBitmap(bitmap);
				currentIndex_++;
			}
			image.postDelayed(new UpdateImage(), 100);
		}
	}
}