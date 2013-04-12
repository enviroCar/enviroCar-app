/* Rebuilds exactly the server application originally developed by
 * Jakob Möllers for his Bachelor thesis
 * Assumes the mongodb has a database named 'odb'
 */
/**************Requires**************/
var express = require('express');
var config = require('./config.json');
var MongoClient = require('mongodb').MongoClient, Server = require('mongodb').Server;

/**************Globals**************/
var app = express();
app.use(express.bodyParser());
var mongoClient = new MongoClient(new Server(config.db.host, config.db.port));

//open the database
mongoClient.open(function(err, mongoClient) {
	var measurements = mongoClient.db('odb').collection('measurements');
	

	app.get('/all', function(req, res) {
		measurements.find({}
		//,{_id:false}
		).toArray(function(err, items) {
			res.json(items);
		});
	});
	
	
	//HTTP POST for uploading measurements
	app.post('/uploadMeasurement', function (req, res) {
		/*
		From php script
		$latitude = $_POST['latitude'];
		$longitude = $_POST['longitude'];
		$throttle_position = $_POST['throttle_position'];
		$rpm = $_POST['rpm'];
		$speed = $_POST['speed'];
		$fuel_type = $_POST['fuel_type'];
		$fuel_consumption = $_POST['fuel_consumption'];
		$intake_pressure = $_POST['intake_pressure'];
		$intake_temperature = $_POST['intake_temperature'];
		$short_term_trim_bank_1 = $_POST['short_term_trim_bank_1'];
		$long_term_trim_bank_1 = $_POST['long_term_trim_bank_1'];
		$maf = $_POST['maf'];
		$measurement_time = $_POST['measurement_time'];
		$engine_load = $_POST['engine_load'];
		$car = $_POST['car'];
		*/
		var objOptions = function (value) {
			return {value:value, writable:true, enumerable:true, configurable:true};
		},
		curr_measurement = function () {
			var input = req.body,
			output = {};
			//validate input
			if(input.hasOwnProperty('latitude') && input.hasOwnProperty('longitude')
				&& input.hasOwnProperty('throttle_position') && input.hasOwnProperty('rpm')
				&& input.hasOwnProperty('rpm') && input.hasOwnProperty('speed')
				&& input.hasOwnProperty('fuel_type') && input.hasOwnProperty('fuel_consumption')
				&& input.hasOwnProperty('intake_pressure') && input.hasOwnProperty('intake_temperature')
				&& input.hasOwnProperty('short_term_trim_bank_1') && input.hasOwnProperty('long_term_trim_bank_1')
				&& input.hasOwnProperty('maf') && input.hasOwnProperty('measurement_time')
				&& input.hasOwnProperty('engine_load') && input.hasOwnProperty('car')){
				
					//validate latitude and longitude
					if(!isNaN(parseFloat(input.latitude)) && !isNaN(parseFloat(input.longitude))){
						//create a GeoJSON Point Object
						var point = { "type": "Point", "coordinates": [input.longitude, input.latitude] };
						//append to the output
						Object.defineProperty(output, 'loc', objOptions(point));
					} else {
						res.send({'error':'An error has occurred'});
					}
					
					//Validate numeric fields (throttle_position, rpm, speed, engine_load, fuel_consumption, intake_pressure, intake_temperature, short_term_trim_bank_1, long_term_trim_bank_1, maf)
					if(!isNaN(parseFloat(input.throttle_position)) && !isNaN(parseFloat(input.rpm))
						&& !isNaN(parseFloat(input.speed)) && !isNaN(parseFloat(input.engine_load))
						&& !isNaN(parseFloat(input.fuel_consumption)) && !isNaN(parseFloat(input.intake_pressure))
						&& !isNaN(parseFloat(input.intake_temperature)) && !isNaN(parseFloat(input.short_term_trim_bank_1))
						&& !isNaN(parseFloat(input.long_term_trim_bank_1)) && !isNaN(parseFloat(input.maf))){
							//append the properties
							Object.defineProperty(output, 'throttle_position', objOptions(input.throttle_position));
							Object.defineProperty(output, 'rpm', objOptions(input.rpm));
							Object.defineProperty(output, 'speed', objOptions(input.speed));
							Object.defineProperty(output, 'engine_load', objOptions(input.engine_load));
							Object.defineProperty(output, 'fuel_consumption', objOptions(input.fuel_consumption));
							Object.defineProperty(output, 'intake_pressure', objOptions(input.intake_pressure));
							Object.defineProperty(output, 'intake_temperature', objOptions(input.intake_temperature));
							Object.defineProperty(output, 'short_term_trim_bank_1', objOptions(input.short_term_trim_bank_1));
							Object.defineProperty(output, 'long_term_trim_bank_1', objOptions(input.long_term_trim_bank_1));
							Object.defineProperty(output, 'maf', objOptions(input.maf));
					} else {
						res.send({'error':'An error has occurred'});
					}
					
					//append measurement time
					Object.defineProperty(output, 'measurement_time', objOptions(new Date(input.measurement_time)));
					
						
			} else {
				res.send({'error':'An error has occurred'});
			}
			return output;
		};
		
		console.log(JSON.stringify(req.body));
		measurements.insert(curr_measurement(), {safe:true}, function (err, result) {
			 if (err) {
				res.send({'error':'An error has occurred'});
			} else {
				console.log('Success: ' + JSON.stringify(result));
				res.send(result[0]);
			}	
		});
	});
});

app.listen(3000);