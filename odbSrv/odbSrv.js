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
		measurements.find({},{_id:false}).toArray(function(err, items) {
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
		var curr_measurement = req.body;
		console.log(JSON.stringify(req.body));
		measurements.insert(curr_measurement, {safe:true}, function (err, result) {
			 if (err) {
				res.send({'error':'An error has occurred'});
			} else {
				console.log('Success: ' + JSON.stringify(result[0]));
				res.send(result[0]);
			}	
		});
	});
});

app.listen(3000);