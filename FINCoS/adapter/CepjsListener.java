package pt.uc.dei.fincos.adapters.cep;

import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import pt.uc.dei.fincos.adapters.OutputListener;
import pt.uc.dei.fincos.sink.Sink;

abstract class CepjsOutputListener extends OutputListener {

	public CepjsOutputListener(String lsnrID, int rtMode, int rtResolution, Sink sinkInstance) {
		super(lsnrID, rtMode, rtResolution, sinkInstance);
	}
	
}

public class CepjsListener extends CepjsOutputListener implements Emitter.Listener {
	
	private Socket socket;
	private String outputStream;

	public CepjsListener(String lsnrID, int rtMode, int rtResolution, Sink sinkInstance,
			Socket socket, String outputStream) {
		super(lsnrID, rtMode, rtResolution, sinkInstance);
		this.socket = socket;
		this.outputStream = outputStream;
	}
	
	@Override
	public void run() {
		this.socket.on(outputStream, this);
	}

	@Override
	public void load() throws Exception {
	}

	@Override
	public synchronized void disconnect() {
		this.socket.off(this.outputStream);
	}

	@Override
	public void call(Object... args) {
		
		JSONObject obj = (JSONObject)args[0];
		
		onOutput(new Object[] {
			obj.get("_eventTypeId"), obj.get("_occurrenceTime"), obj.get("_detectionTime")
		});
		
	}

}
