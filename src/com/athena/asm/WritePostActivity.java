package com.athena.asm;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.athena.asm.util.SmthSupport;
import com.athena.asm.util.StringUtility;
import com.athena.asm.util.task.LoadWritePostTask;
import com.athena.asm.viewmodel.WritePostViewModel;

public class WritePostActivity extends Activity implements OnClickListener,
		OnItemSelectedListener {
	static final int ATTACH_REQUST = 0;

	public static final int TYPE_POST = 0;
	public static final int TYPE_MAIL = 1;
	public static final int TYPE_POST_EDIT = 2;

	private EditText titleEditText;
	private EditText useridEditText;
	private EditText contentEditText;
	private Spinner sigSpinner;
	private Button attachButton;

	public SmthSupport smthSupport;

	private Handler handler = new Handler();

	private WritePostViewModel m_viewModel = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.post_reply);

		smthSupport = SmthSupport.getInstance();
		
		m_viewModel = (WritePostViewModel)getLastNonConfigurationInstance();
		boolean isNewActivity = true;
		if (m_viewModel == null) {
			m_viewModel = new WritePostViewModel();
		}
		else {
			isNewActivity = false;
		}

		titleEditText = (EditText) findViewById(R.id.post_title);
		useridEditText = (EditText) findViewById(R.id.post_userid);
		contentEditText = (EditText) findViewById(R.id.post_content);
		sigSpinner = (Spinner) findViewById(R.id.sig_spinner);

		TextView titleTextView = (TextView) findViewById(R.id.title);

		Button sendButton = (Button) findViewById(R.id.btn_send_post);
		sendButton.setOnClickListener(this);

		attachButton = (Button) findViewById(R.id.btn_attach);
		attachButton.setOnClickListener(this);

		m_viewModel.setToHandlerUrl(getIntent().getStringExtra(StringUtility.URL));
		m_viewModel.setWriteType(getIntent()
				.getIntExtra(StringUtility.WRITE_TYPE, TYPE_POST));
		if (m_viewModel.getWriteType() == TYPE_POST) {
			((LinearLayout) useridEditText.getParent())
					.setVisibility(View.GONE);
			((LinearLayout) useridEditText.getParent()).removeView(sendButton);
			LinearLayout layout = (LinearLayout) findViewById(R.id.post_second_layout);
			layout.addView(sendButton);
			titleTextView.setText("写帖子");
			if (isNewActivity) {
				new LoadWritePostTask(this, m_viewModel).execute();
			}
			else {
				restoreFromViewModel();
			}
			
		} else if (m_viewModel.getWriteType() == TYPE_MAIL) {
			attachButton.setVisibility(View.GONE);
			titleTextView.setText("写  信");
			if (isNewActivity) {
				new LoadWritePostTask(this, m_viewModel).execute();
			}
			else {
				restoreFromViewModel();
			}
			
		} else if (m_viewModel.getWriteType() == TYPE_POST_EDIT) {
			((LinearLayout) useridEditText.getParent())
					.setVisibility(View.GONE);
			((LinearLayout) useridEditText.getParent()).removeView(sendButton);
			LinearLayout layout = (LinearLayout) findViewById(R.id.post_second_layout);
			layout.addView(sendButton);
			titleTextView.setText("修改帖子");
			
			m_viewModel.setPostTitile(getIntent().getStringExtra(StringUtility.TITLE));
			titleEditText.setText(m_viewModel.getPostTitle());
			sigSpinner.setEnabled(false);
			attachButton.setEnabled(false);
			
			if (isNewActivity) {
				new LoadWritePostTask(this, m_viewModel).execute();
			}
			else {
				restoreFromViewModel();
			}
			
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// do nothing to stop onCreated
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		updateViewModel();
		return m_viewModel;
	}

	public void finishWork() {
		createSigSpinner();

		boolean isReply = getIntent().getBooleanExtra(StringUtility.IS_REPLY,
				false);
		if (isReply) {
			contentEditText.requestFocus();
			contentEditText.setSelection(0);
		}
	}

	private void createSigSpinner() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("不使用签名档");
		int sigNum = m_viewModel.getSigNumber();
		for (int i = 1; i <= sigNum; i++) {
			list.add("第" + i + "个");
		}
		if (sigNum > 0) {
			list.add("随机签名档");
		}
		ArrayAdapter<String> sigSpinnerAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		sigSpinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sigSpinner.setAdapter(sigSpinnerAdapter);
		sigSpinner.setOnItemSelectedListener(this);
		if (sigNum > 0) {
			if (m_viewModel.getSelectedSigValue() != -1) {
				sigSpinner.setSelection(m_viewModel.getSelectedSigValue());
			} else {
				sigSpinner.setSelection(sigNum + 1);
			}
		}
	}

	public void parsePostToHandleUrl(String contentString, boolean isReply) {

		if (contentString == null)
			return;
		// function replyForm(board,reid,title,att,signum,sig,ano,outgo,lsave)
		Pattern p = Pattern
				.compile("replyForm\\('[^']+',\\d+,'([^']+)',\\d+,(\\d+),([+-]?\\d+)");
		Matcher m = p.matcher(contentString);
		if (m.find()) {
			String postTitle;
			postTitle = m.group(1);
			m_viewModel.setSigNumber(Integer.parseInt(m.group(2)));
			m_viewModel.setSelectedSigValue(Integer.parseInt(m.group(3)));
			if (!postTitle.contains("Re:") && isReply) {
				postTitle = "Re: " + postTitle;
			}
			titleEditText.setText(postTitle.trim());
			m_viewModel.setPostTitile(postTitle);
		}

		p = Pattern.compile("-->[\\s\\S]*</script>([\\s\\S]*)</textarea>");
		m = p.matcher(contentString);
		if (m.find()) {
			String postContent;
			postContent = m.group(1);
			postContent = postContent.replace("\n", "\n<br/>");
			if (HomeActivity.application.isPromotionShow()) {
				postContent += "--<br/>发送自aSM水木客户端\n<br/>";
			}
			contentEditText.setText(Html.fromHtml("<br/>" + postContent));
			m_viewModel.setPostContent(postContent);
		}

	}
	
	public void parsePostEditToHandleUrl(String contentString) {

		if (contentString == null)
			return;
		// function replyForm(board,reid,title,att,signum,sig,ano,outgo,lsave)
		Pattern pattern = Pattern
				.compile("<textarea[^<>]+>([^<>]+)</textarea>");
		Matcher matcher = pattern.matcher(contentString);
		if (matcher.find()) {
			String postContent;
			postContent = matcher.group(1);
			postContent = postContent.replace("\n", "\n<br/>");
			contentEditText.setText(Html.fromHtml(postContent));
			m_viewModel.setPostContent(postContent);
		}
	}

	public void parseMailToHandleUrl(String contentString) {
		Map<String, String> paramsMap = StringUtility.getUrlParams(m_viewModel.getToHandlerUrl());
		if (paramsMap.containsKey("title")) {
			titleEditText.setText(m_viewModel.getPostTitle());
			Button button = (Button) findViewById(R.id.btn_send_post);
			((LinearLayout) useridEditText.getParent())
					.setVisibility(View.GONE);
			((LinearLayout) useridEditText.getParent()).removeView(button);
			LinearLayout layout = (LinearLayout) findViewById(R.id.post_first_layout);
			layout.addView(button);
		}

		m_viewModel.setSigNumber(StringUtility.getOccur(contentString, "<option") - 2);
		Pattern p = Pattern.compile("option value=\"([+-]?\\d+)\" selected");
		Matcher m = p.matcher(contentString);
		if (m.find()) {
			m_viewModel.setSelectedSigValue(Integer.parseInt(m.group(1)));
		}

		Pattern pattern = Pattern
				.compile("<textarea[^<>]+>([^<>]+)</textarea>");
		Matcher matcher = pattern.matcher(contentString);
		if (matcher.find()) {
			String postContent;
			postContent = matcher.group(1);
			postContent = postContent.replace("\n", "\n<br/>");
			contentEditText.setText(Html.fromHtml("<br />" + postContent));
			m_viewModel.setPostContent(postContent);
		}

		if (paramsMap.containsKey("board")) {
			pattern = Pattern
					.compile("<input class=\"sb1\" type=\"text\" name=\"title\"[^<>]+value=\"([^<>]+)\">");
			matcher = pattern.matcher(contentString);
			if (matcher.find()) {
				String postTitle;
				postTitle = matcher.group(1);
				if (!postTitle.contains("Re:")) {
					postTitle = "Re: " + postTitle;
				}
				titleEditText.setText(postTitle);
				m_viewModel.setPostTitile(postTitle);
			}
			pattern = Pattern
					.compile("<input class=\"sb1\" type=\"text\" name=\"userid\" value=\"([^<>]+)\">");
			matcher = pattern.matcher(contentString);
			if (matcher.find()) {
				useridEditText.setText(matcher.group(1));
			}
		}
	}

	public void showSuccessToast() {
		handler.post(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), "发表成功.",
						Toast.LENGTH_SHORT).show();
			}
		});
		if (m_viewModel.getWriteType() == TYPE_POST) {
			Intent i = new Intent();

			Bundle b = new Bundle();
			b.putBoolean(StringUtility.REFRESH_BOARD, true);
			i.putExtras(b);

			this.setResult(RESULT_OK, i);
		}
		this.finish();
	}

	public void showFailedToast() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "发表失败.",
						Toast.LENGTH_SHORT).show();
			}
		});
		this.finish();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ATTACH_REQUST) {
			if (resultCode == RESULT_OK) {
				attachButton.setEnabled(false);
			}
		}
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.btn_attach) {
			Intent intent = new Intent();
			intent.setClassName("com.athena.asm",
					"com.athena.asm.AttachUploadActivity");
			startActivityForResult(intent, ATTACH_REQUST);
		} else if (view.getId() == R.id.btn_send_post) {
			final ProgressDialog pdialog = new ProgressDialog(this);
			pdialog.setMessage("发表中...");
			pdialog.show();

			updateViewModel();

			((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
					.hideSoftInputFromWindow(contentEditText.getWindowToken(),
							0);

			Thread th = new Thread() {
				@Override
				public void run() {
					boolean result = m_viewModel.sendPost();
					if (!result) {
						showFailedToast();
					} else {
						showSuccessToast();
					}
					pdialog.cancel();
				}
			};
			th.start();
		}
	}
	
	private void updateViewModel() {
		m_viewModel.setPostTitile(titleEditText.getText().toString());
		if (m_viewModel.getMailUserId().length() < 1) {
			m_viewModel.setMailUserId(useridEditText.getText().toString().trim());
		}
		m_viewModel.setPostContent(contentEditText.getText().toString());

		int selectedSig = sigSpinner.getSelectedItemPosition();
		if (selectedSig == m_viewModel.getSigNumber() + 1) {
			selectedSig = -1;
		}
		m_viewModel.setSelectedSigValue(selectedSig);
	}
	
	private void restoreFromViewModel() {
		titleEditText.setText(m_viewModel.getPostTitle());
		useridEditText.setText(m_viewModel.getMailUserId());
		contentEditText.setText(m_viewModel.getPostContent());

		createSigSpinner();		
		int selectedSig = m_viewModel.getSelectedSigValue();
		if (selectedSig == -1) {
			selectedSig = m_viewModel.getSigNumber() + 1;
		}
		sigSpinner.setSelection(selectedSig);
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}
}
