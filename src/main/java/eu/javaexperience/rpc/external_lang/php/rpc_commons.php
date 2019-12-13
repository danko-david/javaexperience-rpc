<?php

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
		return $this->origin->txrx($arr);
	}
}

class PolyApi
{
	private $conn;
	
	public function __construct($conn)
	{
		$this->conn = $conn;
	}
	
	public function __call($name, $args)
	{
		return $this->conn->txrx(array('f' => $name, 'p' => $args));
	}
}
