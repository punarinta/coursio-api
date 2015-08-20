<?php

class CoursioApi
{
    protected $salt;
    protected $publicKey;
    protected $privateKey;
    protected $sessionId = null;

    const ROOT_URL = 'https://t-api.s.coursio.com/api/';

    /**
     * @param $publicKey
     * @param $privateKey
     * @param string $salt
     * @throws Exception
     */
    public function __construct($publicKey, $privateKey, $salt = 'coursio_salt')
    {
        if (!$publicKey || !$privateKey)
        {
            throw new \Exception('Both public and private key are required.');
        }

        $this->salt = $salt;
        $this->publicKey = $publicKey;
        $this->privateKey = $privateKey;
    }

    /**
     * @param $endpoint
     * @param $method
     * @param $data
     * @return mixed
     */
    public function exec($endpoint, $method, $data)
    {
        if (!$this->sessionId)
        {
            // no session => login
            $timestamp = microtime(true);
            $rawString = $this->publicKey . $timestamp . $this->salt;
            $hash = hash_hmac ('sha1', $rawString , $this->privateKey);

            // authenticate via HMAC
            $result = self::curl('auth', 'loginHmac', array
            (
                'apikey'    => $this->publicKey,
                'time'      => $timestamp,
                'random'    => $this->salt,
                'hmac'      => $hash,
            ));

            // save session ID
            $this->sessionId = $result['sessionId'];
        }

        return self::curl($endpoint, $method, $data);
    }

    /**
     * @param $endpoint
     * @param $method
     * @param array $data
     * @return mixed
     * @throws \Exception
     */
    public function curl($endpoint, $method, $data = [])
    {
        $ch = curl_init();

        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_URL, self::ROOT_URL . $endpoint);
        curl_setopt($ch, CURLOPT_HTTPHEADER, ['Token: ' . $this->sessionId]);
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode(['method' => $method, 'data' => $data]));

        $result = curl_exec($ch);
        curl_close($ch);

        if (!($json = json_decode($result, true)))
        {
            throw new \Exception('Wrapper failed to decode message from API 3. Raw data: ' . $result);
        }

        if ($json['isError'])
        {
            // we need to use exceptions here
            throw new \Exception($json['errMsg']);
        }

        return $json['data'];
    }
}
