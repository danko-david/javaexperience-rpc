<?php

class SocketConnector
{
	private $socket;
	
	public function __construct($host, $port)
	{
		$this->socket = fsockopen($host, $port);
	}
	
	public function __descruct()
	{
		socket_close($this->socket);
	}
	
	public function txrx($arr)
	{
        fwrite($this->socket, json_encode($arr) . chr(10));

		$str = fgets($this->socket);
		if($str === false)
		{
			throw new Exception("Rpc connection closed");
		}
		$ret = json_decode($str, true);
		
		if(isset($ret['e']))
		{
			throw new Exception(var_export($ret['e'],true));
		}
		
		return $ret['r'];
	}
}

class NamespaceConnector
{
	private $origin;
	private $ns;
	
	public function __construct($origin, $ns)
	{
		$this->origin = $origin;
		$this->ns = $ns;
	}
	
	public function txrx($arr)
	{
		$arr['N'] = $this->ns;
		return $origin->txrx($arr);
	}
}


