package br.com.soaresdev.bluetoothcommunication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	BluetoothAdapter btAdapter;
	BluetoothSocket socket = null;
	BluetoothSocket tmp = null;
	boolean isConnected = false;
	Ringtone r;
	private NotificationManager nManager;
	private static int APP_ID = 0x01;
	
	
	public void debug(String str) {
		Log.d("DEBUG_TAG", str);
	}
	
	
	public void quitCallback(View view) {
		debug("Clicado no icone de saida");
		
	   new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle("Fechando aplicação")
        .setMessage("Você realmente deseja sair?")
        .setPositiveButton("Sim", new DialogInterface.OnClickListener()
		    {
		        public void onClick(DialogInterface dialog, int which) {
		    		if(isConnected == true) {
		    			//nManager.cancel(APP_ID);
		    		}
		            finish();    
		        }
	
		    })
	    .setNegativeButton("Não", null)
	    .show();   
	}
	
	public void infoCallback(View view) {
		debug("Clicado no icone de informações");
		Toast.makeText(getApplicationContext(), "Informações não disponíveis", Toast.LENGTH_LONG).show();

	}
	
	public void manageBt() {
		debug("Iniciando Manage Bluetooth");
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (btAdapter == null) {
			debug("Não pode criar bluetooth");
			return;
		}

		if (!btAdapter.isEnabled()) {
			debug("Ativando bluetooh!");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, 0x02);
		} else {
			debug("Bluetooth já ativado");
		}
	}
	
	public void closeConn(View view) {
		debug("Fechando conexao");
		try {
			if(this.isConnected == true) {
				debug("Chamando socket close");
				socket.close();
			}
		} catch(IOException e) {
			debug("Falha ao fechar socket: " + e.getMessage());
		}
	}
	
	public void stopAlarm() {
		
		if(r.isPlaying()) {
			debug("Parando alarme...");
			r.stop();
		}
	}
	
	public void playAlarm() {
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		r.play();
	}
	
	public void recoveryControl(View view) {
		debug("NÃO IMPLEMENTADO AINDA");
		Toast.makeText(getApplicationContext(), "Não Implementado ainda", Toast.LENGTH_SHORT).show();
	}
	
	public void vigillanceControl(View view) {
	    ToggleButton t = (ToggleButton)view;
	    boolean on = (boolean) t.isChecked();
	    
	    if(on) {
	    	debug("vigillanceControl ON");
	    	makeConnection();
	    } else {
	    	debug("vigillanceControl OFF");
	    	stopAlarm();
	    	closeConn(view);
	    }
	}

	public void run() {
		
		int toRead = -1;

		debug("Inicou o run");
		
		if(this.isConnected == false) {
			debug("Nao esta conectado");
			return;
		}
		
		//createNotification();
		
        runOnUiThread(new Runnable(){
            public void run(){
            	Toast.makeText(getApplicationContext(), "Sistema conectado. Vigilancia ativada!", Toast.LENGTH_SHORT).show();
            }
        });
        
				
		while(true)  {
			try {
				debug("Try dentro do while");
				
				InputStream inputStream = socket.getInputStream();														
				OutputStream outputStream = socket.getOutputStream();
				byte[] buffer = new byte[4];
		        
		        outputStream.write(new byte[] { (byte) 0xa});		        	
		        	
				debug("Passou pelo Write!");
				
				for(int i = 0; i<4; i++) {
					Thread.sleep(200);
					
					if(i == 3) {
						debug("Nem tenta ler, gera alarme!");
						playAlarm();
						return;
					}
					
					if(inputStream.available() <= 0) {
						debug("Sem dados para ler");
						continue;
					} else {
						debug("Com dados para ler");
						break;
					}
				}
				
				toRead = inputStream.read(buffer);
								
				debug("Bytes lidos: " + String.valueOf(toRead));
	        	if(toRead > 0)
	        		debug(Arrays.toString(buffer));
				
			} catch (IOException e) {
				debug("Excpetion 2: " + e.getMessage());
				playAlarm();
				break;
			} catch (InterruptedException e) {
				debug("Excpetion 244: " + e.getMessage());
				break;
			}
		}
	}
	
	
	public void makeConnection() {
		
		//registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(ActionFoundReceiver, filter);
		
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
			debug("Cancelando scan");
        }
        
    	Toast.makeText(getApplicationContext(), "Procurando Tag. Por Favor Aguarde.", Toast.LENGTH_SHORT).show();
		btAdapter.startDiscovery();
	}
	
	protected void connect(BluetoothDevice device) {
		try {
			debug("Iniciou o connect");
			
			final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
			
			
			Method m = null;
			try {
				m = device.getClass().getMethod("createRfcommSocket",
				        new Class[] { int.class });
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			}
			
		    try {
		    	if(m != null)
		           tmp = (BluetoothSocket) m.invoke(device, 1);
		    	else
		    		tmp = null;
		    } catch (IllegalArgumentException e) {
		        debug(e.getMessage());
		    } catch (IllegalAccessException e) {
		    	debug(e.getMessage());
		    } catch (InvocationTargetException e) {
		    	debug(e.getMessage());
		    }
        
		    if(tmp != null) {
		    	socket = tmp;

				debug("Socket OK =)");

				socket.connect();
		    	
		    } else {
				debug("TMP NULL. Ja esta conectado...");
		    }

			debug("Conectado!");
			
			this.isConnected = true;
		
			this.run();
			
		} catch (final IOException e) {
			debug("Expt: 0145: " + e.getMessage());
		}
			
	}
	
	// Callback do startDiscovery
	private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			debug("OLA MUNDO DA CALLBACK");
			String action = intent.getAction();
			
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				debug("Encontrou: " + device.getName());
				
				if(device.getName().toLowerCase().contains("arduino") == false) {
					debug("Descartando o device: " + device.getName());
					return;
				}
				
				if(device.getBondState() != device.BOND_BONDED) {
					debug("Device " + device.getName() + " encontrado mas não pareado");
					return;
				}
				
				btAdapter.cancelDiscovery();
				
				new Thread() {
					public void run() {
						connect(device);
					};
				}.start();
				
			} else {
				debug("Ação ocorrida: " + action);
				ToggleButton t = (ToggleButton)findViewById(R.id.toggleButton1);
				t.setChecked(false);
            	Toast.makeText(getApplicationContext(), "Tag não encontrada", Toast.LENGTH_SHORT).show();

			}
			
			unregisterReceiver(ActionFoundReceiver);
		}
	};
	
	@Override
	// Função de callback para a ativação do bluetooth
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (requestCode == 0x02) {
			if (resultCode == RESULT_OK) {
				debug("Bluetooth ativado");
			} else if (resultCode == RESULT_CANCELED) {
				debug("Erro enquanto ativava bluetooth...");
			}
		}
	}
	
	public void createNotification() {
		debug("Create notification");
		
	    Intent intent = new Intent(this, MainActivity.class);
	    nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		
		Notification notification = new Notification(R.drawable.main_logo_01,
				"Vigilancia ativada!", System.currentTimeMillis());
		
		notification.setLatestEventInfo(MainActivity.this,
				"BT","Fique tranquilo, vigilancia ativada!",
				PendingIntent.getActivity(this.getBaseContext(), 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT));
		
		nManager.notify(APP_ID, notification);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tag_dev);
		
		Log.d("DEBUG_TAG", "*** INIT ***");
		manageBt();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_tag_dev, menu);
		return true;
	}

}
