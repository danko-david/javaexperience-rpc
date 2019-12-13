<?php 

include('tcp_socket_connector.php');
include('rpc_commons.php');

//connectiong to the 'ExampleRpcServer'
$conn = new SocketConnector('127.0.0.1', 3000); 

$discovery = new PolyApi(new NamespaceConnector($conn, 'DiscoverRpc'));

?>
Available namespaces with PHP source:<br/>
<?php foreach($discovery->getNamespaces() as $ns):?>
	<?=$ns?><br/>
	<pre>
<?=$discovery->source(isset($_GET['lang'])?$_GET['lang']:'php', $ns, null); ?>
	</pre>
	<hr/>
<?php endforeach;?>
<?php

