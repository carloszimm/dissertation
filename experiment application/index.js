const io = require('socket.io')(80);
const events = require('events');
const eventEmitter = new events.EventEmitter();
const fs = require('fs');
const cepjsRx = require('cepjs-rx');
const cepjsMost = require('cepjs-most');

let cepjs;

const chosenLib = process.argv[2];

if(chosenLib == 'most'){
	cepjs = require('cepjs-core')(cepjsMost);
}else if(chosenLib == 'rx'){
	cepjs = require('cepjs-core')(cepjsRx);
}else{
	throw new Error('You must choose a library!');
}

const { all, EventType, fromEvent, merge, minDistance,
		movingToward, patternPolicies, Point, tumblingTimeWindow } = cepjs;
const { order } = patternPolicies;

// writes the pid file
fs.writeFileSync('./pidfile', process.pid);


const inputStreams = ['line55', 'line68', 'line287'];



io.on('connection', socket => {
  console.log('user connected at', Date.now());

  inputStreams.forEach( inputStream => {
  	socket.on(inputStream, data => {
  		eventEmitter.emit(`${inputStream}_stream`, data);
  	});
  });

  socket.on('disconnect', () => {
  	console.log('user disconnected at', Date.now());
  });

});

// An EventType subclass to store bus travel updates
class GpsLocation extends EventType {
	constructor(eventTypeId, eventSource, occcurrenceTime, latitude, longitude){
		super(eventTypeId, eventSource, occcurrenceTime);
		this.location = new Point(latitude, longitude);
	}
}
// Adapts the external event representation to a format accepted in CEP.js
const adaptor = event =>
		new GpsLocation('Bus Movement', 'FINCoS', event.timestamp, event.latitude, event.longitude);
// Sets the order that the movingToward operation should consider
const policies = {
					order: order.OCCURRENCE_TIME
				};

// inputStreams: array representing the supported input streams
inputStreams.forEach(inputStream => {

	// Creates a stream from the event identifier provided in the inputStreams array
	// Events are gathered in time windows of 30 seconds
	let stream = fromEvent(eventEmitter, `${inputStream}_stream`, adaptor)
				.pipe(tumblingTimeWindow(30000));
	// Applies both minDistance and movingToward opertion to the same stream
	// The results of each operator are merged into a single stream
	// Afterwards, a second time window is used to allow further checking
	// in the all operator
	merge(
		stream.pipe(minDistance(['Bus Movement'], new Point(-8.048256, -34.925041),
				'location', minDistance => minDistance <= 500, 'Bus Within Radius')),
		stream.pipe(movingToward(['Bus Movement'], new Point(-8.048256, -34.925041),
				'location', 'Bus Approaching', policies))
	).pipe(tumblingTimeWindow(20000), all(['Bus Within Radius', 'Bus Approaching'], 'Bus Arriving'))
		.subscribe({
			// Subscribe to the stream
			// Complex events are cast under the 'Bus Arriving' event (output stream)
			next: complexEvent => {
				io.emit(complexEvent.eventTypeId, complexEvent);
			}, error: err => {
				console.error(err);
			}
		});
});