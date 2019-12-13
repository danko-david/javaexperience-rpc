<?php 

include('tcp_socket_connector.php');
include('rpc_commons.php');

//note: you can operate persistent only on static queue, because when you close
//the TCP connection (after PHP execution done) the session is dropped.
//So the session lives only unil closeing the connection. 

//connectiong to the 'ExampleRpcServer'
$conn = new SocketConnector('127.0.0.1', 3000); 

$queue = new PolyApi(new NamespaceConnector($conn, 'QueueStorageExampleApi'));

function separate()
{
	echo '<br/><hr/><br/>';
}

/********************************* Pop the queue ******************************/
?>
<form method="POST">
<input type="hidden" name="op" value="pop"/>
<input type="submit" value="Pop tail element"/>
</form>

<?php

if(isset($_POST['op']))
{
	if('pop' == $_POST['op'])
	{
		echo 'Pop tail element: '.$queue->staticTryGetOrNull();
	}
}

separate();

/************************* Add new element to the queue ***********************/

if(isset($_POST['add']))
{
	$queue->staticAdd($_POST['add']);
}

?>
<form method="POST">
	Add new value: <input name="add"/>
	<input type='submit'/>
</form>
<?php

separate();

/************************* Display values in the queue ************************/

echo 'Values in static queue:<br/><br/>'; 
foreach($queue->staticGetAll() as $q)
{
	echo $q;
	echo '<br/>';
}

