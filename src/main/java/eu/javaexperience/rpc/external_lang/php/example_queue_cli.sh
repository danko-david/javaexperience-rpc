#!/usr/bin/php
<?php 
//yes it is actually php but you can run in shell
//run 'watch -n 0.5 ./example_queue_cli.sh list' while adding, popping stuffs
//in the browser using example_queue.php

if(count($argv) < 2 || !in_array($argv[1], array('pop', 'list', 'add')))
{
	echo 'Available commands: pop, list, add $param'."\n";
	die(1);
}

include('tcp_socket_connector.php');
include('rpc_commons.php');

//note: you can operate persistent only on static queue, because when you close
//the TCP connection (after PHP execution done) the session is dropped.
//So the session lives only unil closeing the connection. 

//connectiong to the 'ExampleRpcServer'
$conn = new SocketConnector('127.0.0.1', 3000); 

$queue = new PolyApi(new NamespaceConnector($conn, 'QueueStorageExampleApi'));

switch($argv[1])
{
	case 'pop':
		echo $queue->staticTryGetOrNull();
		echo "\n";
	break;

	case 'list':
	foreach($queue->staticGetAll() as $q)
	{
		echo $q;
		echo "\n";
	}
	break;

	case 'add':
	for($i=2;$i<count($argv);++$i)
	{
		$queue->staticAdd($argv[$i]);
	}
	break;
};

