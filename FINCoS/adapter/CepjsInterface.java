package pt.uc.dei.fincos.adapters.cep;

import java.util.Properties;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

import org.json.JSONObject;

import pt.uc.dei.fincos.basic.Attribute;
import pt.uc.dei.fincos.basic.CSV_Event;
import pt.uc.dei.fincos.basic.Event;
import pt.uc.dei.fincos.basic.Status;
import pt.uc.dei.fincos.basic.Step;
import pt.uc.dei.fincos.sink.Sink;

public class CepjsInterface extends CEP_EngineInterface{
	
	private Socket socket;
	
	private static CepjsInterface instance = null;
	
	public static synchronized CepjsInterface getInstance(Properties connProps,
            int rtMode, int rtResolution) {
        if (instance == null) {
            instance = new CepjsInterface(connProps, rtMode, rtResolution);
        }
        return instance;
    }
	
	private static synchronized void destroyInstance() {
        instance = null;
    }

	public CepjsInterface(Properties connProps, int rtMode, int rtResolution) {
		super(rtMode, rtResolution);
		this.status = new Status(Step.DISCONNECTED, 0);
		this.setConnProperties(connProps);
	}

	@Override
	public synchronized void send(Event e) throws Exception {
		if (this.status.getStep() == Step.READY || this.status.getStep() == Step.CONNECTED) {
			
			// constructs JSON object to be sent
			JSONObject obj = new JSONObject();
			
			// populates the object
			for(Attribute att: e.getAttributes()) {
				obj.put(att.getName(), e.getAttributeValue(att.getName()));
			}
			obj.put("timestamp", e.getTimestamp());
			
			this.socket.emit(e.getType().getName(), obj);
		}
	}

	@Override
	public void send(CSV_Event event) throws Exception {
		
	}

	@Override
	public synchronized boolean connect() throws Exception {
		// informed through the GUI
		String address = this.retrieveConnectionProperty("address");
		
		// sets websocket as the default protocol
		IO.Options options = new IO.Options();
		options.transports = new String[] { WebSocket.NAME };
		
		this.socket = IO.socket("http://" + address, options);
		this.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			  public void call(Object... args) {
				status.setStep(Step.CONNECTED);
			  }
		});
		
		this.socket.connect();
		
		// the above operation is asynchronous
		Thread.sleep(5000);
		
		if(!this.socket.connected()) {
			this.status.setStep(Step.ERROR);
			return false;
		}
		
		return true;
	}

	@Override
	public synchronized boolean load(String[] outputStreams, Sink sinkInstance) throws Exception {
		 // This interface instance has already been loaded
        if (this.status.getStep() == Step.READY) {
            return true;
        } else { // If it is not connected yet, try to connect
            if (!this.socket.connected()) {
                this.connect();
            }
        }
        
        if (this.socket.connected()) {
            this.status.setStep(Step.LOADING);
            
            if (outputStreams != null) {
            	
            	this.outputListeners = new CepjsListener[outputStreams.length];
            	
            	for(int i = 0; i < outputStreams.length; i++) {
            		this.outputListeners[i] =
            				new CepjsListener("lsnr-0", rtMode, rtResolution,
            						sinkInstance, socket, outputStreams[i]);
            	}
            	
            	try {
                    this.startAllListeners();
                } catch (Exception e) {
                    throw new Exception("Could not load event listener (" + e.getMessage() + ").");
                }
            }
            
            this.status.setStep(Step.READY);
            return true;
        }else {
        	return false;
        }
		
	}

	@Override
	public synchronized void disconnect() {
		this.status.setStep(Step.DISCONNECTED);

        //Stops all listeners attached
        stopAllListeners();
        
        this.socket.close();
        
        destroyInstance();
	}

	//optional method
    public String[] getInputStreamList() throws Exception{
    	return new String[0];
    }
    
    //optional method
    public String[] getOutputStreamList() throws Exception{
    	return new String[0];
    }

}
