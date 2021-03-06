package com.athena.asm;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.athena.asm.data.Mail;
import com.athena.asm.data.Subject;
import com.athena.asm.util.StringUtility;

public class SubjectListActivity extends SherlockFragmentActivity
								 implements ProgressDialogProvider, OnOpenActivityFragmentListener {
	
	private ProgressDialog m_pdialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(aSMApplication.THEME);
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.subject_list_activity);	
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		//getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setRequestedOrientation(aSMApplication.ORIENTATION);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// do nothing to stop onCreated
		super.onConfigurationChanged(newConfig);
	}
	
	public void showProgressDialog() {
		if (m_pdialog == null) {
			m_pdialog = new ProgressDialog(this);
			m_pdialog.setMessage("加载版面列表中...");
			m_pdialog.show();
		}
	}
	
	public void dismissProgressDialog() {
		if (m_pdialog != null) {
			m_pdialog.cancel();
			m_pdialog = null;
		}
	}
	
	@Override
	public void onOpenActivityOrFragment(String target, Bundle bundle) {
		if (target.equals(ActivityFragmentTargets.POST_LIST)) {
			Intent intent = new Intent();
			intent.putExtras(bundle);
			intent.setClassName("com.athena.asm", PostListActivity.class.getName());
			startActivityForResult(intent, 0);
		}
	}
}
